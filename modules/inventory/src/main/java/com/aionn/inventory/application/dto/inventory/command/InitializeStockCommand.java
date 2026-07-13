package com.aionn.inventory.application.dto.inventory.command;

import com.aionn.sharedkernel.application.command.Command;

public record InitializeStockCommand(
        String ownerId,
        String skuId,
        String warehouseId,
        int initialQty) implements Command {
}
