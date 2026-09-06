package com.aionn.sharedkernel.integration.event.ordering;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;

/**
 * Emitted when a user adds a SKU to their cart. Scoped to the user rather than the cart because
 * consumers care about the person's intent, not the cart aggregate's lifecycle. The payload carries
 * {@code skuId}; consumers that reason about products resolve it through the catalog query port.
 */
public record CartItemAddedIntegrationEvent(
        String eventId,
        String userId,
        String cartId,
        String skuId,
        int quantity,
        Instant occurredAt) implements IntegrationEvent.UserScoped {

    public CartItemAddedIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
