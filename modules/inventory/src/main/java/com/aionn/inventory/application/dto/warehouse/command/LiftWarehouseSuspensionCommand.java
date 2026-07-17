package com.aionn.inventory.application.dto.warehouse.command;

import com.aionn.sharedkernel.application.command.Command;

public record LiftWarehouseSuspensionCommand(
        String warehouseId,
        String adminId) implements Command {
}
