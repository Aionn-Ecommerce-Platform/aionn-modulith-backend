package com.aionn.catalog.application.port.out.merchant;

import java.time.Instant;
import java.util.List;

/** "Notify me when back in stock" subscriptions keyed by SKU. */
public interface StockAlertPersistencePort {

    void subscribe(String alertId, String userId, String productId, String skuId, Instant createdAt);

    void unsubscribe(String userId, String skuId);

    boolean exists(String userId, String skuId);

    List<PendingAlert> findPendingBySku(String skuId);

    void markNotified(String alertId, Instant notifiedAt);

    record PendingAlert(
            String alertId,
            String userId,
            String productId,
            String skuId,
            Instant createdAt) {
    }
}
