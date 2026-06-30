package com.aionn.sharedkernel.integration.event.inventory;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;

public record StockReservedIntegrationEvent(
        String eventId,
        String reservationId,
        String skuId,
        String warehouseId,
        String orderId,
        int quantity,
        Instant expiresAt,
        Instant occurredAt) implements IntegrationEvent {

    public StockReservedIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
