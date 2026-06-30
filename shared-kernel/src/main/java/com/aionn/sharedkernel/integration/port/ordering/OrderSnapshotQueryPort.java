package com.aionn.sharedkernel.integration.port.ordering;

import java.util.List;
import java.util.Optional;

/**
 * Cross-service port for fetching a flat snapshot of an order. Ordering owns
 * the order aggregate, so consumers (UCP, agent flows, support tools) read
 * through this port instead of importing ordering's domain types.
 */
public interface OrderSnapshotQueryPort {

    Optional<OrderSnapshot> findOrderById(String orderId);

    record OrderSnapshot(
            String orderId,
            String userId,
            String merchantId,
            String currency,
            String status,
            long subtotalMinor,
            long shippingMinor,
            long totalMinor,
            List<Line> lines) {

        public record Line(String skuId, int qty, long unitPriceMinor, long lineTotalMinor) {
        }
    }
}
