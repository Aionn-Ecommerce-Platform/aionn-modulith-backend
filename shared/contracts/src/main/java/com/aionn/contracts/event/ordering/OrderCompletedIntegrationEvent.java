package com.aionn.contracts.event.ordering;

import com.aionn.contracts.event.IntegrationEvent;

import java.time.Instant;

public record OrderCompletedIntegrationEvent(
        String eventId,
        String orderId,
        Instant occurredAt) implements IntegrationEvent.OrderScoped {

    public OrderCompletedIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
