package com.aionn.sharedkernel.integration.event.inventory;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;
import java.util.UUID;

public record SafetyStockBreachedIntegrationEvent(
        String eventId,
        Instant occurredAt,
        String merchantId,
        String skuId,
        String warehouseId,
        int availableQty,
        int safetyStockQty) implements IntegrationEvent {

    public SafetyStockBreachedIntegrationEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID().toString();
        }
    }
}
