package com.aionn.contracts.event.ordering;

import com.aionn.contracts.event.IntegrationEvent;

import java.time.Instant;

public record OrderShippedIntegrationEvent(
        String eventId,
        String orderId,
        String shipmentId,
        Instant occurredAt) implements IntegrationEvent.OrderScoped {

    public OrderShippedIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
