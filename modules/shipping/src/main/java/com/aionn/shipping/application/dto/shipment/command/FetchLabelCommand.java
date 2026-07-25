package com.aionn.shipping.application.dto.shipment.command;

import com.aionn.sharedkernel.application.command.Command;

public record FetchLabelCommand(String shipmentId, String ownerId) implements Command {
}
