package com.aionn.contracts.event.inventory;

import com.aionn.contracts.event.IntegrationEvent;

import java.time.Instant;
import java.util.Objects;

/** A positive stock movement that may make a previously unavailable SKU purchasable again. */
public record StockReplenishedIntegrationEvent(
        String eventId,
        String skuId,
        String warehouseId,
        int quantity,
        Instant occurredAt) implements IntegrationEvent {

    public StockReplenishedIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        skuId = Objects.requireNonNull(skuId, "skuId must not be null");
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
