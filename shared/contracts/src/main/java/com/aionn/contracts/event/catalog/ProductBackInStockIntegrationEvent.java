package com.aionn.contracts.event.catalog;

import com.aionn.contracts.event.IntegrationEvent;

import java.time.Instant;
import java.util.Objects;

/** Notifies one buyer that a SKU they asked about is available again. */
public record ProductBackInStockIntegrationEvent(
        String eventId,
        String userId,
        String productId,
        String skuId,
        String productName,
        Instant occurredAt) implements IntegrationEvent {

    public ProductBackInStockIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        userId = Objects.requireNonNull(userId, "userId must not be null");
        productId = Objects.requireNonNull(productId, "productId must not be null");
        skuId = Objects.requireNonNull(skuId, "skuId must not be null");
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
