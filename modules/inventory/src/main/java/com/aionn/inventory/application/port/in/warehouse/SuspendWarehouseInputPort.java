package com.aionn.inventory.application.port.in.warehouse;

import com.aionn.inventory.application.dto.warehouse.command.SuspendWarehouseCommand;
import com.aionn.inventory.application.dto.warehouse.result.WarehouseResult;

public interface SuspendWarehouseInputPort {
    WarehouseResult execute(SuspendWarehouseCommand command);
}
