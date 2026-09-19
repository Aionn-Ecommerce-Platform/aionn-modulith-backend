package com.aionn.ucp.infrastructure.webhook;

import com.aionn.ucp.application.port.out.UcpWebhookDispatcherPort;
import com.aionn.ucp.application.port.out.UcpWebhookEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * RestClient-backed dispatcher for delivering outbound UCP webhook events to
 * platforms.
 */
@Slf4j
@Component
public class RestClientUcpWebhookDispatcher implements UcpWebhookDispatcherPort {

    private final RestClient restClient;
    private final com.aionn.ucp.application.port.out.UcpMetricsPort metricsPort;

    @Autowired
    public RestClientUcpWebhookDispatcher(
            java.util.Optional<RestClient.Builder> restClientBuilder,
            java.util.Optional<com.aionn.ucp.application.port.out.UcpMetricsPort> metricsPort) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        RestClient.Builder builder = (restClientBuilder != null && restClientBuilder.isPresent())
                ? restClientBuilder.get()
                : RestClient.builder();
        this.restClient = builder
                .requestFactory(requestFactory)
                .build();
        this.metricsPort = metricsPort != null ? metricsPort.orElse(null) : null;
    }

    public RestClientUcpWebhookDispatcher(RestClient restClient) {
        this(restClient, null);
    }

    public RestClientUcpWebhookDispatcher(RestClient restClient,
            com.aionn.ucp.application.port.out.UcpMetricsPort metricsPort) {
        this.restClient = restClient;
        this.metricsPort = metricsPort;
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

            var response = restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(event)
                    .retrieve()
                    .toBodilessEntity();

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
            if (host == null || host.isBlank()) {
                return false;
            }
            String lowerHost = host.toLowerCase();
            if (lowerHost.equals("localhost") || lowerHost.endsWith(".localhost") || lowerHost.endsWith(".local")
                    || lowerHost.endsWith(".internal") || lowerHost.equals("127.0.0.1") || lowerHost.equals("::1")
                    || lowerHost.startsWith("10.") || lowerHost.startsWith("192.168.")
                    || lowerHost.startsWith("172.16.") || lowerHost.startsWith("172.17.")
                    || lowerHost.startsWith("172.18.") || lowerHost.startsWith("172.19.")
                    || lowerHost.startsWith("172.20.") || lowerHost.startsWith("172.21.")
                    || lowerHost.startsWith("172.22.") || lowerHost.startsWith("172.23.")
                    || lowerHost.startsWith("172.24.") || lowerHost.startsWith("172.25.")
                    || lowerHost.startsWith("172.26.") || lowerHost.startsWith("172.27.")
                    || lowerHost.startsWith("172.28.") || lowerHost.startsWith("172.29.")
                    || lowerHost.startsWith("172.30.") || lowerHost.startsWith("172.31.")
                    || lowerHost.startsWith("169.254.")) {
                return false;
            }
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
            } catch (java.net.UnknownHostException e) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
