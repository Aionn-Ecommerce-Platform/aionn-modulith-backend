package com.aionn.inventory.application.dto.warehouse.command;

import com.aionn.sharedkernel.application.command.Command;

public record ChangeStatusCommand(String warehouseId, String ownerId, String status) implements Command {
}
