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
    public RestClientUcpWebhookDispatcher(RestClient.Builder restClientBuilder,
            @Autowired(required = false) com.aionn.ucp.application.port.out.UcpMetricsPort metricsPort) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .build();
        this.metricsPort = metricsPort;
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
}
