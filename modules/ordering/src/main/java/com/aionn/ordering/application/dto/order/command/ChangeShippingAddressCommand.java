package com.aionn.ordering.application.dto.order.command;

import com.aionn.ordering.domain.valueobject.ShippingAddress;
import com.aionn.sharedkernel.application.command.Command;

public record ChangeShippingAddressCommand(
        String orderId,
        String merchantId,
        ShippingAddress newAddress) implements Command {
}
