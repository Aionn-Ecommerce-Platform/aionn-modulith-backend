package com.aionn.sharedkernel.integration.event.catalog;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;
import java.util.List;

/**
 * Emitted when an authenticated user opens a product detail page. Carries the product's brand and
 * categories so behaviour consumers can build affinity profiles without calling back into catalog
 * for every view - views are the highest-volume behavioural signal in the system.
 */
public record ProductViewedIntegrationEvent(
        String eventId,
        String productId,
        String userId,
        String brandId,
        List<String> categoryIds,
        Instant occurredAt) implements IntegrationEvent.ProductScoped {

    public ProductViewedIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        categoryIds = IntegrationEvent.freezeList(categoryIds, "categoryIds");
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
