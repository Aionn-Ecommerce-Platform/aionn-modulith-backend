package com.aionn.ordering.infrastructure.integration.order;

import com.aionn.contracts.port.ordering.OrderQueryPort;
import com.aionn.contracts.port.shipping.ShipmentItemQueryPort;
import com.aionn.ordering.application.port.out.OrderReturnItemPersistencePort;
import com.aionn.ordering.domain.valueobject.OrderStatus;
import com.aionn.ordering.infrastructure.persistence.entity.OrderEntity;
import com.aionn.ordering.infrastructure.persistence.entity.OrderItemEntity;
import com.aionn.ordering.infrastructure.persistence.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OrderingOrderQueryAdapter implements OrderQueryPort {

    private static final int REVIEWABLE_ORDER_PAGE_SIZE = 50;

    private static final List<String> TERMINAL_STATUSES = Arrays.stream(OrderStatus.values())
            .filter(OrderStatus::isTerminal)
            .map(Enum::name)
            .toList();

    private final OrderRepository orderJpaRepository;
    private final ShipmentItemQueryPort shipmentItemQueryPort;
    private final OrderReturnItemPersistencePort orderReturnItemPersistencePort;

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
    public boolean hasOpenOrdersForUser(String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }
        return orderJpaRepository.existsByUserIdAndStatusNotIn(userId, TERMINAL_STATUSES);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReviewablePurchase> findReviewablePurchase(String userId, String orderId, String skuId) {
        if (userId == null || orderId == null || skuId == null) {
            return Optional.empty();
        }
        return orderJpaRepository.findById(orderId)
                .filter(order -> userId.equals(order.getUserId()))
                .flatMap(order -> reviewable(order, skuId));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReviewablePurchase> findLatestReviewablePurchase(
            String userId, Collection<String> skuIds) {
        if (userId == null || skuIds == null || skuIds.isEmpty()) {
            return Optional.empty();
        }
        for (int page = 0; ; page++) {
            List<OrderEntity> candidates = orderJpaRepository.findReviewableOrders(
                    userId, skuIds, PageRequest.of(page, REVIEWABLE_ORDER_PAGE_SIZE));
            if (candidates.isEmpty()) {
                return Optional.empty();
            }
            for (OrderEntity order : candidates) {
                Map<String, Integer> returned = orderReturnItemPersistencePort
                        .returnedQuantities(order.getOrderId());
                List<ShipmentItemQueryPort.DeliveredItem> delivered =
                        shipmentItemQueryPort.deliveredItems(order.getOrderId());
                if (delivered == null) {
                    delivered = List.of();
                }
                for (OrderItemEntity item : order.getItems()) {
                    if (!skuIds.contains(item.getId().getSkuId())) {
                        continue;
                    }
                    Optional<ReviewablePurchase> purchase =
                            reviewable(order, item, returned, delivered);
                    if (purchase.isPresent()) {
                        return purchase;
                    }
                }
            }
            if (candidates.size() < REVIEWABLE_ORDER_PAGE_SIZE) {
                return Optional.empty();
            }
        }
    }

    private Optional<ReviewablePurchase> reviewable(OrderEntity order, String skuId) {
        OrderItemEntity item = order.getItems().stream()
                .filter(line -> skuId.equals(line.getId().getSkuId()))
                .findFirst()
                .orElse(null);
        if (item == null) {
            return Optional.empty();
        }
        Map<String, Integer> returned = orderReturnItemPersistencePort
                .returnedQuantities(order.getOrderId());
        List<ShipmentItemQueryPort.DeliveredItem> delivered =
                shipmentItemQueryPort.deliveredItems(order.getOrderId());
        if (delivered == null) {
            delivered = List.of();
        }
        return reviewable(order, item, returned, delivered);
    }

    private Optional<ReviewablePurchase> reviewable(
            OrderEntity order,
            OrderItemEntity item,
            Map<String, Integer> returned,
            List<ShipmentItemQueryPort.DeliveredItem> delivered) {
        String skuId = item.getId().getSkuId();
        int returnedQty = returned.getOrDefault(skuId, 0);
        int deliveredQty = delivered.stream()
                .filter(line -> skuId.equals(line.skuId()))
                .mapToInt(ShipmentItemQueryPort.DeliveredItem::qty)
                .sum();
        Instant deliveredAt = delivered.stream()
                .filter(line -> skuId.equals(line.skuId()))
                .map(ShipmentItemQueryPort.DeliveredItem::deliveredAt)
                .filter(java.util.Objects::nonNull)
                .max(Instant::compareTo)
                .orElse(null);
        if (deliveredQty == 0 && OrderStatus.COMPLETED.name().equals(order.getStatus())
                && order.getCompletedAt() != null) {
            // Legacy orders placed before shipment line tracking: use order completion
            // as the item delivery signal.
            deliveredQty = item.getQty();
            deliveredAt = order.getCompletedAt();
        }
        if (deliveredQty <= returnedQty) {
            return Optional.empty();
        }
        return Optional.of(new ReviewablePurchase(order.getOrderId(), skuId, deliveredAt));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderSummary> findOrderSummary(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            return Optional.empty();
        }
        return orderJpaRepository.findById(orderId)
                .map(e -> new OrderSummary(
                        e.getOrderId(),
                        e.getUserId(),
                        e.getMerchantId(),
                        e.getTotalAmount(),
                        e.getCurrency(),
                        e.getStatus()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderPurchase> findOrderPurchase(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            return Optional.empty();
        }
        return orderJpaRepository.findById(orderId)
                .map(e -> new OrderPurchase(
                        e.getUserId(),
                        e.getItems().stream()
                                .map(item -> item.getId().getSkuId())
                                .distinct()
                                .toList()));
    }
}
