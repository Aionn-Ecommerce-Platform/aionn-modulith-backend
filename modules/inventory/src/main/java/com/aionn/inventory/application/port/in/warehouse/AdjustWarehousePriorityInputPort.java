package com.aionn.inventory.application.port.in.warehouse;

import com.aionn.inventory.application.dto.warehouse.command.AdjustPriorityCommand;
import com.aionn.inventory.application.dto.warehouse.result.WarehouseResult;

public interface AdjustWarehousePriorityInputPort {
    WarehouseResult execute(AdjustPriorityCommand command);
}
