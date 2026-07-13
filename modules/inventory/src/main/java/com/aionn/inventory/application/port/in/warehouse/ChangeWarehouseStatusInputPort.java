package com.aionn.inventory.application.port.in.warehouse;

import com.aionn.inventory.application.dto.warehouse.command.ChangeStatusCommand;
import com.aionn.inventory.application.dto.warehouse.result.WarehouseResult;

public interface ChangeWarehouseStatusInputPort {
    WarehouseResult execute(ChangeStatusCommand command);
}
