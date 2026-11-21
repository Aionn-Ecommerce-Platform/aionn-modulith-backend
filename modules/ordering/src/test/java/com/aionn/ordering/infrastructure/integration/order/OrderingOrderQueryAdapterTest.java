package com.aionn.ordering.infrastructure.integration.order;

import com.aionn.contracts.port.ordering.OrderQueryPort;
import com.aionn.contracts.port.shipping.ShipmentItemQueryPort;
import com.aionn.ordering.application.port.out.OrderReturnItemPersistencePort;
import com.aionn.ordering.infrastructure.persistence.entity.OrderEntity;
import com.aionn.ordering.infrastructure.persistence.entity.OrderItemEntity;
import com.aionn.ordering.infrastructure.persistence.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderingOrderQueryAdapterTest {

    @Mock
    private OrderRepository orderJpaRepository;
    @Mock
    private ShipmentItemQueryPort shipmentItemQueryPort;
    @Mock
    private OrderReturnItemPersistencePort orderReturnItemPersistencePort;

    @InjectMocks
    private OrderingOrderQueryAdapter adapter;

    @Test
    void hasOpenOrdersForMerchantReturnsFalseWhenBlank() {
        assertFalse(adapter.hasOpenOrdersForMerchant(null));
        assertFalse(adapter.hasOpenOrdersForMerchant("  "));
    }

    @Test
    void hasOpenOrdersForMerchantQueriesRepository() {
        when(orderJpaRepository.existsByMerchantIdAndStatusNotIn(eq("m-1"), anyList())).thenReturn(true);

        assertTrue(adapter.hasOpenOrdersForMerchant("m-1"));
    }

    @Test
    void hasOpenOrdersForUserReturnsFalseWhenBlank() {
        assertFalse(adapter.hasOpenOrdersForUser(null));
        assertFalse(adapter.hasOpenOrdersForUser("  "));
    }

    @Test
    void hasOpenOrdersForUserQueriesRepository() {
        when(orderJpaRepository.existsByUserIdAndStatusNotIn(eq("usr-1"), anyList())).thenReturn(true);

        assertTrue(adapter.hasOpenOrdersForUser("usr-1"));
    }

    @Test
    void findReviewablePurchaseRequiresDeliveredQuantityAboveReturnedQuantity() {
        OrderEntity entity = order("ord-1", "COMPLETED", Instant.now());
        entity.setItems(List.of(orderItem("ord-1", "sku-1", 1)));
        when(orderJpaRepository.findById("ord-1")).thenReturn(Optional.of(entity));
        when(shipmentItemQueryPort.deliveredItems("ord-1"))
                .thenReturn(List.of(new ShipmentItemQueryPort.DeliveredItem(
                        "sku-1", 1, Instant.parse("2026-01-02T00:00:00Z"))));
        when(orderReturnItemPersistencePort.returnedQuantities("ord-1")).thenReturn(Map.of());

        Optional<OrderQueryPort.ReviewablePurchase> purchase =
                adapter.findReviewablePurchase("u-1", "ord-1", "sku-1");

        assertTrue(purchase.isPresent());
        assertEquals("sku-1", purchase.get().skuId());
        assertEquals(Instant.parse("2026-01-02T00:00:00Z"), purchase.get().deliveredAt());
    }

    @Test
    void findReviewablePurchaseExcludesFullyReturnedItem() {
        OrderEntity entity = order("ord-1", "COMPLETED", Instant.now());
        entity.setItems(List.of(orderItem("ord-1", "sku-1", 1)));
        when(orderJpaRepository.findById("ord-1")).thenReturn(Optional.of(entity));
        when(shipmentItemQueryPort.deliveredItems("ord-1"))
                .thenReturn(List.of(new ShipmentItemQueryPort.DeliveredItem("sku-1", 1, Instant.now())));
        when(orderReturnItemPersistencePort.returnedQuantities("ord-1"))
                .thenReturn(Map.of("sku-1", 1));

        assertTrue(adapter.findReviewablePurchase("u-1", "ord-1", "sku-1").isEmpty());
    }

    @Test
    void findReviewablePurchaseFallsBackToCompletedOrderForLegacyShipments() {
        Instant completedAt = Instant.parse("2026-01-03T00:00:00Z");
        OrderEntity entity = order("ord-1", "COMPLETED", completedAt);
        entity.setItems(List.of(orderItem("ord-1", "sku-1", 2)));
        when(orderJpaRepository.findById("ord-1")).thenReturn(Optional.of(entity));
        when(shipmentItemQueryPort.deliveredItems("ord-1")).thenReturn(List.of());
        when(orderReturnItemPersistencePort.returnedQuantities("ord-1")).thenReturn(Map.of());

        Optional<OrderQueryPort.ReviewablePurchase> purchase =
                adapter.findReviewablePurchase("u-1", "ord-1", "sku-1");

        assertTrue(purchase.isPresent());
        assertEquals(completedAt, purchase.get().deliveredAt());
    }

    @Test
    void findLatestReviewablePurchaseReturnsNewestEligibleOrder() {
        OrderEntity entity = order("ord-1", "COMPLETED", Instant.now());
        entity.setItems(List.of(orderItem("ord-1", "sku-1", 1)));
        when(orderJpaRepository.findReviewableOrders(
                eq("u-1"), eq(List.of("sku-1")), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(entity));
        when(shipmentItemQueryPort.deliveredItems("ord-1")).thenReturn(List.of());
        when(orderReturnItemPersistencePort.returnedQuantities("ord-1")).thenReturn(Map.of());

        assertTrue(adapter.findLatestReviewablePurchase("u-1", List.of("sku-1")).isPresent());
    }

    @Test
    void findOrderSummaryReturnsMappedSummary() {
        assertTrue(adapter.findOrderSummary(null).isEmpty());

        OrderEntity entity = new OrderEntity();
        entity.setOrderId("ord-1");
        entity.setUserId("u-1");
        entity.setMerchantId("m-1");
        entity.setTotalAmount(BigDecimal.valueOf(200));
        entity.setCurrency("VND");

        when(orderJpaRepository.findById("ord-1")).thenReturn(Optional.of(entity));

        Optional<OrderQueryPort.OrderSummary> summary = adapter.findOrderSummary("ord-1");

        assertTrue(summary.isPresent());
        assertEquals("ord-1", summary.get().orderId());
        assertEquals("u-1", summary.get().userId());
        assertEquals("m-1", summary.get().merchantId());
        assertEquals(BigDecimal.valueOf(200), summary.get().totalAmount());
        assertEquals("VND", summary.get().currency());
    }

    @Test
    void findOrderPurchaseReturnsTheBuyerAndDistinctSkuIds() {
        assertTrue(adapter.findOrderPurchase(null).isEmpty());

        OrderEntity entity = new OrderEntity();
        entity.setOrderId("ord-1");
        entity.setUserId("u-1");
        entity.setItems(List.of(
                orderItem("ord-1", "sku-1"),
                orderItem("ord-1", "sku-2"),
                orderItem("ord-1", "sku-1")));

        when(orderJpaRepository.findById("ord-1")).thenReturn(Optional.of(entity));

        Optional<OrderQueryPort.OrderPurchase> purchase = adapter.findOrderPurchase("ord-1");

        assertTrue(purchase.isPresent());
        assertEquals("u-1", purchase.get().userId());
        assertEquals(List.of("sku-1", "sku-2"), purchase.get().skuIds());
    }

    private static OrderEntity order(String orderId, String status, Instant completedAt) {
        OrderEntity entity = new OrderEntity();
        entity.setOrderId(orderId);
        entity.setUserId("u-1");
        entity.setStatus(status);
        entity.setCompletedAt(completedAt);
        return entity;
    }

    private static OrderItemEntity orderItem(String orderId, String skuId) {
        return orderItem(orderId, skuId, 1);
    }

    private static OrderItemEntity orderItem(String orderId, String skuId, int qty) {
        OrderItemEntity entity = new OrderItemEntity();
        entity.setId(new OrderItemEntity.OrderItemId(orderId, skuId));
        entity.setQty(qty);
        return entity;
    }
}
