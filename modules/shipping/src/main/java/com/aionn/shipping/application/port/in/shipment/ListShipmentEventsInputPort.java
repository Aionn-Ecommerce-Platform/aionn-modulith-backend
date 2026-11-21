package com.aionn.shipping.application.port.in.shipment;

import com.aionn.shipping.application.dto.shipment.result.ShipmentEventResult;

import java.util.List;

public interface ListShipmentEventsInputPort {
    List<ShipmentEventResult> execute(String shipmentId, String requesterUserId);
}
