package com.aionn.ordering.application.dto.order.command;

import com.aionn.sharedkernel.application.command.Command;

public record ConfirmDeliveredCommand(String orderId) implements Command {
}
