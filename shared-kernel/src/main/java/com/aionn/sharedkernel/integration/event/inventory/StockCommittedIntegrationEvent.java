package com.aionn.sharedkernel.integration.event.inventory;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;
import java.util.UUID;

public record StockCommittedIntegrationEvent(
        String eventId,
        String reservationId,
        String skuId,
        String warehouseId,
        String orderId,
        int quantity,
        Instant occurredAt) implements IntegrationEvent {

    public StockCommittedIntegrationEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID().toString();
        }
    }
}
