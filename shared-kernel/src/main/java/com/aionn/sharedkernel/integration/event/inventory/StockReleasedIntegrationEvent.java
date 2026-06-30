package com.aionn.sharedkernel.integration.event.inventory;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;
import java.util.UUID;

public record StockReleasedIntegrationEvent(
        String eventId,
        String reservationId,
        String skuId,
        String warehouseId,
        String orderId,
        int quantity,
        String reason,
        Instant occurredAt) implements IntegrationEvent {

    public StockReleasedIntegrationEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID().toString();
        }
    }
}
