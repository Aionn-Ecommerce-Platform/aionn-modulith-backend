package com.aionn.ordering.application.dto.order.command;

import com.aionn.ordering.domain.valueobject.ShippingAddress;
import com.aionn.sharedkernel.application.command.Command;

import java.util.List;

public record PlaceOrderCommand(
        String userId,
        String addressId,
        String paymentMethodId,
        String currency,
        ShippingAddress shippingAddressSnapshot,
        List<String> selectedSkuIds,
        String gateway) implements Command {
}
