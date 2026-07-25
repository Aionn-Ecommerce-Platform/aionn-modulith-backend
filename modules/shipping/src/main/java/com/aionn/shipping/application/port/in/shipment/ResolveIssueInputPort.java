package com.aionn.shipping.application.port.in.shipment;

import com.aionn.shipping.application.dto.shipment.command.ResolveIssueCommand;
import com.aionn.shipping.application.dto.shipment.result.ShipmentResult;

public interface ResolveIssueInputPort {
    ShipmentResult execute(ResolveIssueCommand command);
}
