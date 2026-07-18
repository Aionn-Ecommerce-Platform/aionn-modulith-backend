package com.aionn.ordering.application.dto.order.command;

import com.aionn.sharedkernel.application.command.Command;

public record ConfirmPreparationCommand(String orderId, String ownerId) implements Command {
}
