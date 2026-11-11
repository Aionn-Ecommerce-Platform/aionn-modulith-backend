package com.aionn.contracts.event.inventory;

import com.aionn.contracts.event.IntegrationEvent;

import java.time.Instant;

public record SafetyStockBreachedIntegrationEvent(
        String eventId,
        Instant occurredAt,
        String merchantId,
        String skuId,
        String warehouseId,
        int availableQty,
        int safetyStockQty) implements IntegrationEvent.WarehouseScoped {

    public SafetyStockBreachedIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
