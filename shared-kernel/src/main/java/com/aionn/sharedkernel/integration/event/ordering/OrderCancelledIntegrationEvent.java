package com.aionn.sharedkernel.integration.event.ordering;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;

public record OrderCancelledIntegrationEvent(
        String eventId,
        String orderId,
        String reasonCode,
        String reason,
        CancellationType cancellationType,
        Instant occurredAt) implements IntegrationEvent {

    public OrderCancelledIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }

    public enum CancellationType {
        USER_CANCELLED,
        AUTO_CANCELLED,
        MERCHANT_REJECTED
    }
}
