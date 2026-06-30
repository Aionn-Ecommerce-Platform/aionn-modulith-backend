package com.aionn.sharedkernel.integration.event.inventory;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;
import java.util.UUID;

public record StockReservationFailedIntegrationEvent(
        String eventId,
        String skuId,
        String warehouseId,
        String orderId,
        int quantity,
        String reason,
        Instant occurredAt) implements IntegrationEvent {

    public StockReservationFailedIntegrationEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID().toString();
        }
    }
}
