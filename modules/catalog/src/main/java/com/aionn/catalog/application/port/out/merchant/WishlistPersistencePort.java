package com.aionn.catalog.application.port.out.merchant;

import java.time.Instant;
import java.util.List;

/** Persistence for the buyer's saved products. */
public interface WishlistPersistencePort {

    void add(String userId, String productId, Instant addedAt);

    void remove(String userId, String productId);

    boolean exists(String userId, String productId);

    List<String> findProductIds(String userId, int offset, int limit);

    long count(String userId);
}
