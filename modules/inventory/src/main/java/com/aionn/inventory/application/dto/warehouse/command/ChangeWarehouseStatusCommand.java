package com.aionn.inventory.application.dto.warehouse.command;

import com.aionn.inventory.domain.valueobject.WarehouseStatus;
import com.aionn.sharedkernel.application.command.Command;

public record ChangeWarehouseStatusCommand(
        String warehouseId,
        String merchantId,
        WarehouseStatus status) implements Command {
}
