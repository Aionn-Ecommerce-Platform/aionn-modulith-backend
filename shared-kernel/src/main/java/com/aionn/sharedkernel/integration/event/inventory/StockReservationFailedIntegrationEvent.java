package com.aionn.sharedkernel.integration.event.inventory;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;

public record StockReservationFailedIntegrationEvent(
        String eventId,
        String skuId,
        String warehouseId,
        String orderId,
        int quantity,
        String reason,
        Instant occurredAt) implements IntegrationEvent {

    public StockReservationFailedIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
