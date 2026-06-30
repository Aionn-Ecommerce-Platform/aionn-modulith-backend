package com.aionn.sharedkernel.integration.event.catalog;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;

public record ProductEmergencyTakedownIntegrationEvent(
        String eventId,
        String productId,
        String adminId,
        String reason,
        Instant occurredAt) implements IntegrationEvent {

    public ProductEmergencyTakedownIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
