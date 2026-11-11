package com.aionn.ucp.infrastructure.webhook;

import com.aionn.sharedkernel.util.RetryBackoff;
import com.aionn.ucp.application.port.out.UcpWebhookDispatcherPort;
import com.aionn.ucp.application.port.out.UcpWebhookEvent;
import com.aionn.ucp.infrastructure.config.UcpWebhookProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class UcpWebhookDeliveryScheduler {

    private final UcpWebhookDeliveryStore store;
    private final UcpWebhookDispatcherPort dispatcher;
    private final UcpWebhookProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${aionn.ucp.webhook.poll-delay-ms:5000}")
    @SchedulerLock(name = "ucp-webhook-delivery", lockAtMostFor = "PT5M", lockAtLeastFor = "PT5S")
    public void deliverPending() {
        for (UcpWebhookDeliveryStore.PendingDelivery delivery : store.findPending(properties.batchSize())) {
            deliver(delivery);
        }
    }

    private void deliver(UcpWebhookDeliveryStore.PendingDelivery delivery) {
        try {
            UcpWebhookEvent event = objectMapper.readValue(delivery.payload(), UcpWebhookEvent.class);
            if (dispatcher.dispatch(delivery.webhookUrl(), event)) {
                store.markDelivered(delivery.deliveryId());
                return;
            }
            fail(delivery, "Webhook endpoint rejected the delivery");
        } catch (Exception exception) {
            fail(delivery, exception.getMessage());
        }
    }

    private void fail(UcpWebhookDeliveryStore.PendingDelivery delivery, String error) {
        int attempts = delivery.attempts() + 1;
        boolean dead = attempts >= properties.maxAttempts();
        long backoffSeconds = RetryBackoff.seconds(attempts);
        store.markFailed(delivery.deliveryId(), attempts,
                clock.instant().plus(Duration.ofSeconds(backoffSeconds)), error, dead);
        if (dead) {
            log.error("UCP webhook delivery {} exhausted {} attempts: {}", delivery.deliveryId(), attempts, error);
        } else {
            log.warn("UCP webhook delivery {} failed (attempt {}), retrying in {}s: {}",
                    delivery.deliveryId(), attempts, backoffSeconds, error);
        }
    }
}
