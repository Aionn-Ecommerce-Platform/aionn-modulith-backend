package com.aionn.sharedkernel.integration.event.inventory;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;

public record SafetyStockBreachedIntegrationEvent(
        String eventId,
        Instant occurredAt,
        String merchantId,
        String skuId,
        String warehouseId,
        int availableQty,
        int safetyStockQty) implements IntegrationEvent {

    public SafetyStockBreachedIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
