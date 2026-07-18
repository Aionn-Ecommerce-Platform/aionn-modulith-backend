package com.aionn.ordering.application.dto.order.command;

import com.aionn.ordering.domain.valueobject.ShippingAddress;
import com.aionn.sharedkernel.application.command.Command;

import java.math.BigDecimal;

public record ChangeShippingInfoCommand(
        String orderId,
        String userId,
        ShippingAddress newAddress,
        BigDecimal newShippingFee) implements Command {
}
