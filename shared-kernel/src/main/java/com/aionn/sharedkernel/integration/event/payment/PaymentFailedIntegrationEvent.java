package com.aionn.sharedkernel.integration.event.payment;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;

public record PaymentFailedIntegrationEvent(
        String eventId,
        String paymentId,
        String orderId,
        String errorCode,
        String reason,
        Instant occurredAt) implements IntegrationEvent {

    public PaymentFailedIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
