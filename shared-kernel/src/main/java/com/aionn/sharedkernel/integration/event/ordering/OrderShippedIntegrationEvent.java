package com.aionn.sharedkernel.integration.event.ordering;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;

public record OrderShippedIntegrationEvent(
        String eventId,
        String orderId,
        String shipmentId,
        Instant occurredAt) implements IntegrationEvent {

    public OrderShippedIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
