package com.aionn.catalog.application.dto.product.command;

import com.aionn.sharedkernel.application.command.Command;

public record EmergencyTakedownCommand(String productId, String adminId, String reason) implements Command {
}
