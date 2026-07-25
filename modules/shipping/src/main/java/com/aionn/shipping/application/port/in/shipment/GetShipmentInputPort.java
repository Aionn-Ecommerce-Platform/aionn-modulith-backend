package com.aionn.shipping.application.port.in.shipment;

import com.aionn.shipping.application.dto.shipment.result.ShipmentResult;

public interface GetShipmentInputPort {
    ShipmentResult execute(String shipmentId, String requesterUserId);
}
