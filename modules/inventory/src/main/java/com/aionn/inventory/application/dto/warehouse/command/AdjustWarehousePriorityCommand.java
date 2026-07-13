package com.aionn.inventory.application.dto.warehouse.command;

import com.aionn.sharedkernel.application.command.Command;

public record AdjustWarehousePriorityCommand(
        String warehouseId,
        String merchantId,
        int priorityLevel) implements Command {
}
