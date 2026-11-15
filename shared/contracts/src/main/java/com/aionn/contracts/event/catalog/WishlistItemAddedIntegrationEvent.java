package com.aionn.contracts.event.catalog;

import com.aionn.contracts.event.IntegrationEvent;

import java.time.Instant;

public record WishlistItemAddedIntegrationEvent(
        String eventId,
        String userId,
        String productId,
        Instant occurredAt) implements IntegrationEvent.ProductScoped {

    public WishlistItemAddedIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
