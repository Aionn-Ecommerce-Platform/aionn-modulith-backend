package com.aionn.shipping.application.dto.shipment.command;

import com.aionn.shipping.domain.valueobject.ShipmentAddress;
import com.aionn.shipping.domain.valueobject.ShipmentDimensions;
import com.aionn.sharedkernel.application.command.Command;

public record QuoteShippingCommand(
        String orderId,
        ShipmentAddress address,
        ShipmentDimensions dimensions,
        String currency) implements Command {
}
