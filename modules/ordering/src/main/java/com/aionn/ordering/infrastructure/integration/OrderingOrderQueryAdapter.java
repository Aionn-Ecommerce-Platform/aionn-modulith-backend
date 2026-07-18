package com.aionn.ordering.infrastructure.integration;

import com.aionn.ordering.domain.valueobject.OrderStatus;
import com.aionn.ordering.infrastructure.persistence.repository.OrderRepository;
import com.aionn.sharedkernel.integration.port.ordering.OrderQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderingOrderQueryAdapter implements OrderQueryPort {

    private static final List<String> TERMINAL_STATUSES = Arrays.stream(OrderStatus.values())
            .filter(OrderStatus::isTerminal)
            .map(Enum::name)
            .toList();

    private final OrderRepository orderJpaRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean hasOpenOrdersForMerchant(String merchantId) {
        if (merchantId == null || merchantId.isBlank()) {
            return false;
        }
        return orderJpaRepository.existsByMerchantIdAndStatusNotIn(merchantId, TERMINAL_STATUSES);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasCompletedPurchaseForSkus(String userId, java.util.Collection<String> skuIds) {
        if (userId == null || userId.isBlank() || skuIds == null || skuIds.isEmpty()) {
            return false;
        }
        return orderJpaRepository.existsCompletedPurchaseForSkus(userId, skuIds);
    }

    @Override
    @Transactional(readOnly = true)
    public String findCompletedOrderIdForSkus(String userId, java.util.Collection<String> skuIds) {
        if (userId == null || userId.isBlank() || skuIds == null || skuIds.isEmpty()) {
            return null;
        }
        return orderJpaRepository.findCompletedOrderIdForSkus(userId, skuIds);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<OrderSummary> findOrderSummary(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            return java.util.Optional.empty();
        }
        return orderJpaRepository.findById(orderId)
                .map(e -> new OrderSummary(
                        e.getOrderId(),
                        e.getMerchantId(),
                        e.getTotalAmount(),
                        e.getCurrency()));
    }
}
