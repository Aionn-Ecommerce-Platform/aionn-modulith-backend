package com.aionn.ordering.infrastructure.integration.order;

import com.aionn.ordering.application.dto.order.command.PlaceOrderHeadlessCommand;
import com.aionn.ordering.application.service.OrderService;
import com.aionn.ordering.domain.model.Order;
import com.aionn.ordering.domain.model.OrderItem;
import com.aionn.ordering.domain.valueobject.OrderStatus;
import com.aionn.ordering.domain.valueobject.ShippingAddress;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.sharedkernel.integration.port.ordering.OrderPlacementPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderingOrderPlacementAdapterTest {

        @Mock
        private OrderService orderService;

        @InjectMocks
        private OrderingOrderPlacementAdapter adapter;

        @Test
        void placeHeadlessDelegatesToOrderService() {
                OrderPlacementPort.PlaceCommand.Line line = new OrderPlacementPort.PlaceCommand.Line("sku-1", 2);
                OrderPlacementPort.PlaceCommand.ShippingAddress addr = new OrderPlacementPort.PlaceCommand.ShippingAddress(
                                "a-1", "John", "+84912345678", "12 Main St", "W", "D", "P", "VN");
                OrderPlacementPort.PlaceCommand command = new OrderPlacementPort.PlaceCommand(
                                "usr-1", List.of(line), "VOUCHER", "pm-1", "VND", BigDecimal.TEN, addr);

                Order order = new Order("ord-1", null, "usr-1", "m-1", "prop-1",
                                "pm-1", "VND",
                                List.of(new OrderItem("sku-1", 2, Money.of(BigDecimal.valueOf(95), "VND"), "wh-1",
                                                null)),
                                new ShippingAddress("a-1", "John", "+84912345678", "12 Main St", "W", "D", "P", "VN"),
                                Money.of(BigDecimal.TEN, "VND"), Money.of(BigDecimal.valueOf(200), "VND"),
                                OrderStatus.PENDING, "pay-1", null, Instant.now(), Instant.now(), null, null);

                when(orderService.placeOrderHeadless(any(PlaceOrderHeadlessCommand.class))).thenReturn(order);

                OrderPlacementPort.PlacedOrder placed = adapter.placeHeadless(command);

                assertEquals("ord-1", placed.orderId());
                assertEquals(200L, placed.totalAmountMinor());
        }
}
