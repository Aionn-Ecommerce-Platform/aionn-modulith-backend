package com.aionn.ordering.infrastructure.integration.order;

import com.aionn.ordering.application.dto.order.command.PlaceOrderHeadlessCommand;
import com.aionn.ordering.application.service.OrderService;
import com.aionn.ordering.domain.model.Order;
import com.aionn.ordering.domain.valueobject.ShippingAddress;
import com.aionn.sharedkernel.integration.port.ordering.OrderPlacementPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ordering-side adapter that satisfies the cross-service placement port by
 * rebuilding ordering's internal headless command. Domain types stay inside
 * ordering -- the caller never imports {@code PlaceOrderHeadlessCommand} or
 * {@code ShippingAddress}.
 */
@Component
@RequiredArgsConstructor
public class OrderingOrderPlacementAdapter implements OrderPlacementPort {

        private final OrderService orderService;

        @Override
        public PlacedOrder placeHeadless(PlaceCommand command) {
                List<PlaceOrderHeadlessCommand.Line> lines = command.lines().stream()
                                .map(l -> new PlaceOrderHeadlessCommand.Line(l.skuId(), l.qty()))
                                .toList();
                ShippingAddress addr = command.shippingAddress() == null ? null
                                : new ShippingAddress(
                                                command.shippingAddress().addressId(),
                                                command.shippingAddress().contactName(),
                                                command.shippingAddress().phone(),
                                                command.shippingAddress().detailAddress(),
                                                command.shippingAddress().wardCode(),
                                                command.shippingAddress().districtCode(),
                                                command.shippingAddress().provinceCode(),
                                                command.shippingAddress().countryCode());
                Order order = orderService.placeOrderHeadless(new PlaceOrderHeadlessCommand(
                                command.userId(),
                                lines,
                                command.voucherCode(),
                                command.paymentMethodId(),
                                command.currency(),
                                addr,
                                command.idempotencyKey()));
                long total = order.getTotalAmount() == null ? 0L : order.getTotalAmount().amount().longValue();
                return new PlacedOrder(order.getOrderId(), total, order.getCurrency(), order.getStatus().name());
        }
}
