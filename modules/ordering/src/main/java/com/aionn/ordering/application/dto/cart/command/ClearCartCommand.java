package com.aionn.ordering.application.dto.cart.command;

import com.aionn.sharedkernel.application.command.Command;

public record ClearCartCommand(String userId, String reason) implements Command {
}
