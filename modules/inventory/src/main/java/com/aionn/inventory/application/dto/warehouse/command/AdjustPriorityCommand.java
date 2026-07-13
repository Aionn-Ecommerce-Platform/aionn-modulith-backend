package com.aionn.inventory.application.dto.warehouse.command;

import com.aionn.sharedkernel.application.command.Command;

public record AdjustPriorityCommand(String warehouseId, String ownerId, int priorityLevel) implements Command {
}
