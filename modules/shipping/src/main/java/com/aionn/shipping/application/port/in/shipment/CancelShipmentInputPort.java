package com.aionn.shipping.application.port.in.shipment;

import com.aionn.shipping.application.dto.shipment.command.CancelShipmentCommand;
import com.aionn.shipping.application.dto.shipment.result.ShipmentResult;

public interface CancelShipmentInputPort {
    ShipmentResult execute(CancelShipmentCommand command);
}
