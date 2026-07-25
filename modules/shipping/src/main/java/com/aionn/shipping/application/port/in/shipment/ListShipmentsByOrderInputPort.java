package com.aionn.shipping.application.port.in.shipment;

import com.aionn.shipping.application.dto.shipment.result.ShipmentResult;
import java.util.List;

public interface ListShipmentsByOrderInputPort {
    List<ShipmentResult> execute(String orderId, String requesterUserId);
}
