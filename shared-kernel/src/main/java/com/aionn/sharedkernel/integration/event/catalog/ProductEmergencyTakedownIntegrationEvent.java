package com.aionn.sharedkernel.integration.event.catalog;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;
import java.util.UUID;

public record ProductEmergencyTakedownIntegrationEvent(
        String eventId,
        String productId,
        String adminId,
        String reason,
        Instant occurredAt) implements IntegrationEvent {

    public ProductEmergencyTakedownIntegrationEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID().toString();
        }
    }
}
