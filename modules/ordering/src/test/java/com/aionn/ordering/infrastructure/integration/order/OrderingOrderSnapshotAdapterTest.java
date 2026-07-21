package com.aionn.ordering.infrastructure.integration.order;

import com.aionn.ordering.application.port.out.OrderPersistencePort;
import com.aionn.ordering.domain.model.Order;
import com.aionn.ordering.domain.model.OrderItem;
import com.aionn.ordering.domain.valueobject.ShippingAddress;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.sharedkernel.integration.port.ordering.OrderSnapshotQueryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderingOrderSnapshotAdapterTest {

    @Mock
    private OrderPersistencePort orderRepository;

    @InjectMocks
    private OrderingOrderSnapshotAdapter adapter;

    private static ShippingAddress address() {
        return new ShippingAddress("a-1", "John", "+84912345678", "12 Main St", "WARD", "DIST", "PROV", "VN");
    }

    private static OrderItem item() {
        return new OrderItem("sku-1", 2, Money.of(BigDecimal.valueOf(100), "VND"), "wh-1", "res-1");
    }

    @Test
    void findOrderByIdReturnsMappedSnapshot() {
        Order order = Order.place("ord-1", "usr-1", "m-1", "prop-1", "pm-1", "VND",
                List.of(item()), address(), Money.zero("VND"), Money.of(BigDecimal.valueOf(200), "VND"), Instant.now());

        when(orderRepository.findById("ord-1")).thenReturn(Optional.of(order));

        Optional<OrderSnapshotQueryPort.OrderSnapshot> snapshotOpt = adapter.findOrderById("ord-1");

        assertTrue(snapshotOpt.isPresent());
        OrderSnapshotQueryPort.OrderSnapshot snapshot = snapshotOpt.get();
        assertEquals("ord-1", snapshot.orderId());
        assertEquals("usr-1", snapshot.userId());
        assertEquals("m-1", snapshot.merchantId());
        assertEquals("VND", snapshot.currency());
        assertEquals("PENDING", snapshot.status());
        assertEquals(200L, snapshot.subtotalMinor());
        assertEquals(0L, snapshot.shippingMinor());
        assertEquals(200L, snapshot.totalMinor());
        assertEquals(1, snapshot.lines().size());
        assertEquals("sku-1", snapshot.lines().get(0).skuId());
    }
}
