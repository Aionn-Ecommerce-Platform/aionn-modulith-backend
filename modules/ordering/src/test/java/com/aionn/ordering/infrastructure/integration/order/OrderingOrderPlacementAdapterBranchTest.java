package com.aionn.ordering.infrastructure.integration.order;

import java.time.Instant;
import java.util.List;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.aionn.ordering.application.dto.order.command.PlaceOrderHeadlessCommand;
import com.aionn.ordering.application.service.OrderService;
import com.aionn.ordering.domain.model.Order;
import com.aionn.ordering.domain.valueobject.OrderStatus;
import com.aionn.contracts.port.ordering.OrderPlacementPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


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
                                "usr-1", List.of(), "VOUCHER", "pm-1", "VND", null);

                Order order = new Order("ord-1", null, "usr-1", "m-1", "prop-1", "pm-1", "VND",
                                List.of(), null, null, null,
                                OrderStatus.PENDING, "pay-1", null,
                                Instant.now(), Instant.now(), null, null);

                Mockito
                                .when(orderService.placeOrderHeadless(
                                                ArgumentMatchers.any(PlaceOrderHeadlessCommand.class)))
                                .thenReturn(order);

                OrderPlacementPort.PlacedOrder placed = adapter.placeHeadless(command);

                assertEquals("ord-1", placed.orderId());
                assertEquals(0L, placed.totalAmountMinor());
        }
}
