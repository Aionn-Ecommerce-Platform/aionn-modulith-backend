package com.aionn.shipping.application.port.in.shipment;

import com.aionn.shipping.application.dto.shipment.result.ShipmentResult;

public interface RegisterShipmentInputPort {
    ShipmentResult execute(String shipmentId);
}
