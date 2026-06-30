package com.aionn.sharedkernel.integration.event.payment;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;
import java.util.UUID;

public record PaymentFailedIntegrationEvent(
        String eventId,
        String paymentId,
        String orderId,
        String errorCode,
        String reason,
        Instant occurredAt) implements IntegrationEvent {

    public PaymentFailedIntegrationEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID().toString();
        }
    }
}
