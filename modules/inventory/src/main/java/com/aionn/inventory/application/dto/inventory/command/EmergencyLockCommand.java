package com.aionn.inventory.application.dto.inventory.command;

import com.aionn.sharedkernel.application.command.Command;

public record EmergencyLockCommand(
        String adminId,
        String skuId,
        String warehouseId,
        String reason) implements Command {
}
