package com.aionn.sharedkernel.integration.port.ordering;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Cross-service cart operations port.
 * Allows protocol adapters (such as UCP) to manipulate cart sessions
 * backed by ordering's persistence without leaking domain aggregates.
 */
public interface CartOperationsPort {

    CartSnapshot getOrCreateCart(String cartId, String userId);

    Optional<CartSnapshot> findCartById(String cartId);

    CartSnapshot saveCartItems(String cartId, String userId, Map<String, Integer> skuQuantities);

    CartSnapshot clearCart(String cartId, String userId);

    record CartSnapshot(
            String cartId,
            String userId,
            Map<String, Integer> items,
            String voucherCode,
            Instant createdAt,
            Instant updatedAt) {

        public CartSnapshot {
            items = items == null ? Map.of() : Collections.unmodifiableMap(items);
        }
    }
}
