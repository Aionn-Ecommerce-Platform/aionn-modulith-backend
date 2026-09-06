package com.aionn.ordering.application.port.out.integration;

import java.time.Instant;

/**
 * Cart facts other modules subscribe to. Kept separate from
 * {@link OrderingIntegrationEventPublisherPort}, which covers the order lifecycle: cart activity is a
 * behavioural signal, not a step in the fulfilment saga.
 */
public interface CartIntegrationEventPublisherPort {

    void publishCartItemAdded(
            String cartId, String userId, String skuId, int quantity, Instant occurredAt);
}
