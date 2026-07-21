package com.aionn.ordering.infrastructure.integration.order;

import com.aionn.ordering.application.dto.order.command.PlaceOrderHeadlessCommand;
import com.aionn.ordering.application.dto.order.result.OrderResult;
import com.aionn.ordering.application.service.OrderService;
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

        OrderResult result = new OrderResult("ord-1", null, "usr-1", "m-1", "prop-1",
                "pm-1", "pay-1", "VND", BigDecimal.valueOf(200), BigDecimal.TEN,
                "a-1", "John", "+84912345678", "12 Main St", "W", "D", "P", "VN",
                List.of(), "PLACED", null, Instant.now(), Instant.now(), null, null);

        when(orderService.placeOrderHeadless(any(PlaceOrderHeadlessCommand.class))).thenReturn(result);

        OrderPlacementPort.PlacedOrder placed = adapter.placeHeadless(command);

        assertEquals("ord-1", placed.orderId());
        assertEquals(200L, placed.totalAmountMinor());
    }
}
