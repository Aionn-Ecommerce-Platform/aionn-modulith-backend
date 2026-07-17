package com.aionn.inventory.application.dto.inventory.command;

import com.aionn.sharedkernel.application.command.Command;

public record ConfigureSafetyStockCommand(
        String ownerId,
        String skuId,
        String warehouseId,
        int safetyStockQty) implements Command {
}
