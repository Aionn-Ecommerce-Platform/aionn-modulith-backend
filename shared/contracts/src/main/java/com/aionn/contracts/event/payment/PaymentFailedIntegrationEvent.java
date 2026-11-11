package com.aionn.contracts.event.payment;

import com.aionn.contracts.event.IntegrationEvent;

import java.time.Instant;

public record PaymentFailedIntegrationEvent(
        String eventId,
        String paymentId,
        String orderId,
        String errorCode,
        String reason,
        Instant occurredAt) implements IntegrationEvent.PaymentScoped {

    public PaymentFailedIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
