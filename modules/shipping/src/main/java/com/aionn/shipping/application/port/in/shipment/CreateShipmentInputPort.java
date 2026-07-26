package com.aionn.shipping.application.port.in.shipment;

import com.aionn.shipping.application.dto.shipment.command.CreateShipmentCommand;
import com.aionn.shipping.application.dto.shipment.result.ShipmentResult;

public interface CreateShipmentInputPort {
    ShipmentResult execute(CreateShipmentCommand command);
}
