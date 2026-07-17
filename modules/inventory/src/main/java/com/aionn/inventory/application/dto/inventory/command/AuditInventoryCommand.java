package com.aionn.inventory.application.dto.inventory.command;

import com.aionn.sharedkernel.application.command.Command;

public record AuditInventoryCommand(
        String ownerId,
        String skuId,
        String warehouseId,
        int actualQty) implements Command {
}
