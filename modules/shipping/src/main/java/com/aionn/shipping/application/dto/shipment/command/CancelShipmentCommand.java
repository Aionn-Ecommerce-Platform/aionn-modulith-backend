package com.aionn.shipping.application.dto.shipment.command;

import com.aionn.sharedkernel.application.command.Command;

public record CancelShipmentCommand(String shipmentId, String reason, String ownerId) implements Command {
}
