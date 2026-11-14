package com.aionn.contracts.port.ordering;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;

public interface OrderQueryPort {

    boolean hasOpenOrdersForMerchant(String merchantId);

    boolean hasOpenOrdersForUser(String userId);

    boolean hasCompletedPurchaseForSkus(String userId, Collection<String> skuIds);

    String findCompletedOrderIdForSkus(String userId, Collection<String> skuIds);

    Optional<OrderSummary> findOrderSummary(String orderId);

    record OrderSummary(String orderId, String userId, String merchantId, BigDecimal totalAmount, String currency,
            String status) {

        public OrderSummary(String orderId, String userId, String merchantId, BigDecimal totalAmount,
                String currency) {
            this(orderId, userId, merchantId, totalAmount, currency, null);
        }
    }
}
