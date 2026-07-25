package com.aionn.shipping.application.port.in.shipment;

import com.aionn.shipping.application.dto.shipment.command.FetchLabelCommand;
import com.aionn.shipping.application.dto.shipment.result.ShipmentResult;

public interface FetchLabelInputPort {
    ShipmentResult execute(FetchLabelCommand command);
}
