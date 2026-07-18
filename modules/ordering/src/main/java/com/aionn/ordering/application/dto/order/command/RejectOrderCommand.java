package com.aionn.ordering.application.dto.order.command;

import com.aionn.sharedkernel.application.command.Command;

public record RejectOrderCommand(String orderId, String ownerId, String reason) implements Command {
}
