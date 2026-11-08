package com.aionn.ucp.infrastructure.webhook;

import com.aionn.ucp.application.port.out.UcpWebhookDispatcherPort;
import com.aionn.ucp.application.port.out.UcpWebhookEvent;
import com.aionn.ucp.infrastructure.config.UcpProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Dispatches outbound UCP webhook events, signed so a platform can reject forged or replayed
 * deliveries. When {@code aionn.ucp.webhook-secret} is set it sends {@code X-UCP-Timestamp}
 * (unix seconds) and {@code X-UCP-Signature: t=<ts>,v1=<hmac-sha256-hex>} over
 * {@code "<ts>.<raw-body>"}. Without a secret the headers are omitted (unsigned, dev only).
 */
@Slf4j
@Component
public class RestClientUcpWebhookDispatcher implements UcpWebhookDispatcherPort {

    static final String HEADER_SIGNATURE = "X-UCP-Signature";
    static final String HEADER_TIMESTAMP = "X-UCP-Timestamp";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_SCHEME = "v1";

    private final RestClient restClient;
    private final com.aionn.ucp.application.port.out.UcpMetricsPort metricsPort;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final byte[] signingSecret; // null when unsigned

    @Autowired
    public RestClientUcpWebhookDispatcher(
            java.util.Optional<RestClient.Builder> restClientBuilder,
            java.util.Optional<com.aionn.ucp.application.port.out.UcpMetricsPort> metricsPort,
            java.util.Optional<UcpProperties> properties,
            java.util.Optional<Clock> clock) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        RestClient.Builder builder = restClientBuilder.orElseGet(RestClient::builder);
        this.restClient = builder
                .requestFactory(requestFactory)
                .build();
        this.metricsPort = metricsPort.orElse(null);
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
        this.clock = clock.orElseGet(Clock::systemUTC);
        this.signingSecret = resolveSecret(properties.map(UcpProperties::webhookSecret).orElse(null));
    }

    public RestClientUcpWebhookDispatcher(RestClient restClient) {
        this(restClient, null, null, Clock.systemUTC());
    }

    public RestClientUcpWebhookDispatcher(RestClient restClient,
            com.aionn.ucp.application.port.out.UcpMetricsPort metricsPort) {
        this(restClient, metricsPort, null, Clock.systemUTC());
    }

    public RestClientUcpWebhookDispatcher(RestClient restClient,
            com.aionn.ucp.application.port.out.UcpMetricsPort metricsPort,
            String webhookSecret, Clock clock) {
        this.restClient = restClient;
        this.metricsPort = metricsPort;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
        this.clock = clock != null ? clock : Clock.systemUTC();
        this.signingSecret = resolveSecret(webhookSecret);
    }

    private static byte[] resolveSecret(String secret) {
        return (secret != null && !secret.isBlank())
                ? secret.getBytes(StandardCharsets.UTF_8)
                : null;
    }

    @Override
    public boolean dispatch(String webhookUrl, UcpWebhookEvent event) {
        if (webhookUrl == null || webhookUrl.isBlank() || event == null) {
            log.warn("Skipping webhook dispatch due to missing URL or event payload");
            return false;
        }

        if (!isSafeWebhookUrl(webhookUrl)) {
            log.warn("Rejecting webhook dispatch to prohibited destination: {}", webhookUrl);
            return false;
        }

        try {
            log.info("Dispatching UCP webhook event '{}' for order '{}' to {}",
                    event.eventType(), event.orderId(), webhookUrl);

            // Serialize once: the signature must cover exactly the bytes on the wire.
            String body = objectMapper.writeValueAsString(event);

            var requestSpec = restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);

            if (signingSecret != null) {
                long timestamp = clock.instant().getEpochSecond();
                String signature = sign(timestamp, body);
                requestSpec = requestSpec
                        .header(HEADER_TIMESTAMP, String.valueOf(timestamp))
                        .header(HEADER_SIGNATURE, "t=" + timestamp + "," + SIGNATURE_SCHEME + "=" + signature);
            }

            var response = requestSpec.retrieve().toBodilessEntity();

            boolean success = response.getStatusCode().is2xxSuccessful();
            if (metricsPort != null) {
                metricsPort.recordWebhookDispatch(event.eventType(), success);
            }
            if (success) {
                log.debug("Successfully delivered UCP webhook event '{}' to {}", event.eventId(), webhookUrl);
            } else {
                log.warn("Webhook endpoint returned status {} for event '{}'", response.getStatusCode(),
                        event.eventId());
            }
            return success;
        } catch (Exception e) {
            log.warn("Failed to deliver UCP webhook event '{}' to {}: {}",
                    event.eventId(), webhookUrl, e.getMessage());
            if (metricsPort != null) {
                metricsPort.recordWebhookDispatch(event.eventType(), false);
            }
            return false;
        }
    }

    static boolean isSafeWebhookUrl(String webhookUrl) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return false;
        }
        try {
            java.net.URI uri = java.net.URI.create(webhookUrl);
            String scheme = uri.getScheme();
            if (scheme == null || (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme))) {
                return false;
            }
            String host = uri.getHost();
            if (host == null || host.isBlank() || isProhibitedHost(host.toLowerCase())) {
                return false;
            }
            return areResolvedAddressesSafe(host);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isProhibitedHost(String lowerHost) {
        if (lowerHost.equals("localhost") || lowerHost.endsWith(".localhost") || lowerHost.endsWith(".local")
                || lowerHost.endsWith(".internal") || lowerHost.equals("127.0.0.1") || lowerHost.equals("::1")
                || lowerHost.startsWith("10.") || lowerHost.startsWith("192.168.")
                || lowerHost.startsWith("169.254.")) {
            return true;
        }
        for (int i = 16; i <= 31; i++) {
            if (lowerHost.startsWith("172." + i + ".")) {
                return true;
            }
        }
        return false;
    }

    private static boolean areResolvedAddressesSafe(String host) {
        try {
            java.net.InetAddress[] addresses = java.net.InetAddress.getAllByName(host);
            if (addresses == null || addresses.length == 0) {
                return false;
            }
            for (java.net.InetAddress addr : addresses) {
                if (addr.isLoopbackAddress() || addr.isSiteLocalAddress()
                        || addr.isLinkLocalAddress() || addr.isMulticastAddress()
                        || addr.isAnyLocalAddress()) {
                    return false;
                }
            }
            return true;
        } catch (java.net.UnknownHostException e) {
            return false;
        }
    }

    /** HMAC-SHA256 over {@code "<timestamp>.<body>"} (Stripe/GitHub convention); the timestamp gives a replay window. */
    private String sign(long timestamp, String body) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingSecret, HMAC_ALGORITHM));
            byte[] digest = mac.doFinal((timestamp + "." + body).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            // HmacSHA256 is a JDK-required algorithm; a null/empty key was filtered
            // at construction. Reaching here is a programming error.
            throw new IllegalStateException("Cannot sign UCP webhook payload", e);
        }
    }
}
