package com.aionn.sharedkernel.integration.event.inventory;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;

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
        eventId = IntegrationEvent.requireEventId(eventId);
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
