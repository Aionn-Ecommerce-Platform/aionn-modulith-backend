package com.aionn.inventory.application.dto.warehouse.command;

import com.aionn.sharedkernel.application.command.Command;

public record CreateWarehouseCommand(String merchantId, String address, int priorityLevel) implements Command {
}
