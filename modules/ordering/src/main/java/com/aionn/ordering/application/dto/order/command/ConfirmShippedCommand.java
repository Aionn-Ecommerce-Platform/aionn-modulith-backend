package com.aionn.ordering.application.dto.order.command;

import com.aionn.sharedkernel.application.command.Command;

public record ConfirmShippedCommand(String orderId, String shipmentId) implements Command {
}
