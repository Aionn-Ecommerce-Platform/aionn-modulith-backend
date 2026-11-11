package com.aionn.contracts.event.catalog;

import com.aionn.contracts.event.IntegrationEvent;

import java.time.Instant;

public record ProductEmergencyTakedownIntegrationEvent(
        String eventId,
        String productId,
        String adminId,
        String reason,
        Instant occurredAt) implements IntegrationEvent.ProductScoped {

    public ProductEmergencyTakedownIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
