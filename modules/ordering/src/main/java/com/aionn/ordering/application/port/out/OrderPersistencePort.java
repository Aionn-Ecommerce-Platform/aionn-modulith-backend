package com.aionn.ordering.application.port.out;

import com.aionn.ordering.domain.model.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderPersistencePort {

    Order save(Order order);

    Optional<Order> findById(String orderId);

    List<Order> findByUser(String userId, int limit);

    List<Order> findByUserAndStatuses(String userId, List<String> statuses, int limit);

    List<Order> findByMerchant(String merchantId, int limit);

    List<Order> findByMerchantAndStatuses(String merchantId, List<String> statuses, int limit);

    List<OrderAnalyticsRow> findMerchantAnalyticsRows(String merchantId, Instant from, Instant to);

    List<PlatformAnalyticsRow> findPlatformAnalyticsRows(Instant from, Instant to);

    List<TopProductRow> findMerchantTopProductRows(String merchantId, Instant from, Instant to);

    /** Sweep: order ids still PENDING past the deadline (used by auto-cancel). */
    List<String> findPendingOrderIdsOlderThan(Instant cutoff, int limit);

    record OrderAnalyticsRow(
            String status,
            BigDecimal totalAmount,
            String currency,
            Instant createdAt) {
    }

    record PlatformAnalyticsRow(
            String merchantId,
            String status,
            BigDecimal totalAmount,
            String currency,
            Instant createdAt) {
    }

    record TopProductRow(
            String skuId,
            long unitsSold,
            BigDecimal revenue) {
    }
}
