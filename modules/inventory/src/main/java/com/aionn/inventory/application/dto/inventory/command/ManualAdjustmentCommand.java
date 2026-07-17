package com.aionn.inventory.application.dto.inventory.command;

import com.aionn.inventory.domain.valueobject.AdjustmentType;
import com.aionn.sharedkernel.application.command.Command;

public record ManualAdjustmentCommand(
        String ownerId,
        String skuId,
        String warehouseId,
        int qty,
        AdjustmentType type,
        String reason) implements Command {
}
