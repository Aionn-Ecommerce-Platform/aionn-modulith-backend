package com.aionn.inventory.application.dto.inventory.command;

import com.aionn.sharedkernel.application.command.Command;

public record EmergencyUnlockCommand(
        String adminId,
        String skuId,
        String warehouseId) implements Command {
}
