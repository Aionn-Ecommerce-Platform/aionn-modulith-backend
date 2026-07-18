package com.aionn.ordering.application.dto.order.command;

import com.aionn.sharedkernel.application.command.Command;

public record CancelOrderCommand(String orderId, String userId, String reason) implements Command {
}
