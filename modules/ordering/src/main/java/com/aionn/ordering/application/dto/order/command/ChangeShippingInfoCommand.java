package com.aionn.ordering.application.dto.order.command;

import com.aionn.ordering.domain.valueobject.ShippingAddress;
import com.aionn.sharedkernel.application.command.Command;

public record ChangeShippingInfoCommand(
        String orderId,
        String userId,
        ShippingAddress newAddress) implements Command {
}
