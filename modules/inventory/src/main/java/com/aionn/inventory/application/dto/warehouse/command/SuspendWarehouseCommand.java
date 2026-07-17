package com.aionn.inventory.application.dto.warehouse.command;

import com.aionn.sharedkernel.application.command.Command;

public record SuspendWarehouseCommand(String warehouseId, String adminId, String reason) implements Command {
}
