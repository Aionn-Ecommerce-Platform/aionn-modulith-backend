package com.aionn.sharedkernel.integration.event.ordering;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;

public record OrderCompletedIntegrationEvent(
        String eventId,
        String orderId,
        Instant occurredAt) implements IntegrationEvent {

    public OrderCompletedIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
