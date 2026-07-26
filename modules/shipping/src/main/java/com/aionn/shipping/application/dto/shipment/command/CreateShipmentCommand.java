package com.aionn.shipping.application.dto.shipment.command;

import com.aionn.shipping.domain.valueobject.ShipmentAddress;
import com.aionn.shipping.domain.valueobject.ShipmentDimensions;
import com.aionn.sharedkernel.application.command.Command;

import java.math.BigDecimal;

public record CreateShipmentCommand(
                String orderId,
                String merchantId,
                String ownerId,
                String userId,
                ShipmentAddress address,
                ShipmentDimensions dimensions,
                BigDecimal codAmount,
                BigDecimal shippingFee,
                String currency) implements Command {
}
