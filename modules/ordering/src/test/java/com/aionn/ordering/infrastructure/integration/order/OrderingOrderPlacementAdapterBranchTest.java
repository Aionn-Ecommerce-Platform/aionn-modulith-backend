package com.aionn.ordering.infrastructure.integration.order;

import com.aionn.ordering.application.dto.order.command.PlaceOrderHeadlessCommand;
import com.aionn.ordering.application.service.OrderService;
import com.aionn.ordering.domain.model.Order;
import com.aionn.ordering.domain.valueobject.OrderStatus;
import com.aionn.sharedkernel.integration.port.ordering.OrderPlacementPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class OrderingOrderPlacementAdapterBranchTest {

        @Mock
        private OrderService orderService;

        @InjectMocks
        private OrderingOrderPlacementAdapter adapter;

        @Test
        void placeHeadlessHandlesNullShippingAddress() {
                OrderPlacementPort.PlaceCommand command = new OrderPlacementPort.PlaceCommand(
                                "usr-1", java.util.List.of(), "VOUCHER", "pm-1", "VND", BigDecimal.TEN, null);

                Order order = new Order("ord-1", null, "usr-1", "m-1", "prop-1", "pm-1", "VND",
                                java.util.List.of(), null, null, null,
                                OrderStatus.PENDING, "pay-1", null,
                                java.time.Instant.now(), java.time.Instant.now(), null, null);

                org.mockito.Mockito
                                .when(orderService.placeOrderHeadless(
                                                org.mockito.ArgumentMatchers.any(PlaceOrderHeadlessCommand.class)))
                                .thenReturn(order);

                OrderPlacementPort.PlacedOrder placed = adapter.placeHeadless(command);

                assertEquals("ord-1", placed.orderId());
                assertEquals(0L, placed.totalAmountMinor());
        }
}
