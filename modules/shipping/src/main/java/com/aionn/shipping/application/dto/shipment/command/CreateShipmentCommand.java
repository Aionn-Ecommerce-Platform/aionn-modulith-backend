package com.aionn.shipping.application.dto.shipment.command;

import com.aionn.shipping.domain.valueobject.ShipmentAddress;
import com.aionn.sharedkernel.application.command.Command;

import java.math.BigDecimal;
import java.util.List;

public record CreateShipmentCommand(
                String orderId,
                String merchantId,
                String ownerId,
                String userId,
                ShipmentAddress address,
                List<QuoteShippingCommand.Parcel> parcels,
                BigDecimal codAmount,
                BigDecimal shippingFee,
                String currency) implements Command {
}
