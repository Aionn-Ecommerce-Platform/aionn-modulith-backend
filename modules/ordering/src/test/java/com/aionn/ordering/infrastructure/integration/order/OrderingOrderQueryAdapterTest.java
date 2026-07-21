package com.aionn.ordering.infrastructure.integration.order;

import com.aionn.ordering.infrastructure.persistence.entity.OrderEntity;
import com.aionn.ordering.infrastructure.persistence.repository.OrderRepository;
import com.aionn.sharedkernel.integration.port.ordering.OrderQueryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderingOrderQueryAdapterTest {

    @Mock
    private OrderRepository orderJpaRepository;

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
    void hasCompletedPurchaseForSkusQueriesRepository() {
        assertFalse(adapter.hasCompletedPurchaseForSkus(null, List.of("sku-1")));
        when(orderJpaRepository.existsCompletedPurchaseForSkus("usr-1", List.of("sku-1"))).thenReturn(true);

        assertTrue(adapter.hasCompletedPurchaseForSkus("usr-1", List.of("sku-1")));
    }

    @Test
    void findCompletedOrderIdForSkusQueriesRepository() {
        assertNull(adapter.findCompletedOrderIdForSkus(null, List.of("sku-1")));
        when(orderJpaRepository.findCompletedOrderIdForSkus("usr-1", List.of("sku-1"))).thenReturn("ord-1");

        assertEquals("ord-1", adapter.findCompletedOrderIdForSkus("usr-1", List.of("sku-1")));
    }

    @Test
    void findOrderSummaryReturnsMappedSummary() {
        assertTrue(adapter.findOrderSummary(null).isEmpty());

        OrderEntity entity = new OrderEntity();
        entity.setOrderId("ord-1");
        entity.setMerchantId("m-1");
        entity.setTotalAmount(BigDecimal.valueOf(200));
        entity.setCurrency("VND");

        when(orderJpaRepository.findById("ord-1")).thenReturn(Optional.of(entity));

        Optional<OrderQueryPort.OrderSummary> summary = adapter.findOrderSummary("ord-1");

        assertTrue(summary.isPresent());
        assertEquals("ord-1", summary.get().orderId());
        assertEquals("m-1", summary.get().merchantId());
        assertEquals(BigDecimal.valueOf(200), summary.get().totalAmount());
        assertEquals("VND", summary.get().currency());
    }
}
