package com.aionn.inventory.application.port.in.warehouse;

import com.aionn.inventory.application.dto.warehouse.command.CreateWarehouseCommand;
import com.aionn.inventory.application.dto.warehouse.result.WarehouseResult;

public interface CreateWarehouseInputPort {
    WarehouseResult execute(CreateWarehouseCommand command);
}
