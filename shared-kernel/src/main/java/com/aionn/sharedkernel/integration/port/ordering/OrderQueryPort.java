package com.aionn.sharedkernel.integration.port.ordering;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;

public interface OrderQueryPort {

    boolean hasOpenOrdersForMerchant(String merchantId);

    boolean hasCompletedPurchaseForSkus(String userId, Collection<String> skuIds);

    String findCompletedOrderIdForSkus(String userId, Collection<String> skuIds);

    Optional<OrderSummary> findOrderSummary(String orderId);

    record OrderSummary(String orderId, String merchantId, BigDecimal totalAmount, String currency) {
    }
}
