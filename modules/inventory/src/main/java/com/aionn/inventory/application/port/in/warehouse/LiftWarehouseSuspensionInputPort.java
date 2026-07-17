package com.aionn.inventory.application.port.in.warehouse;

import com.aionn.inventory.application.dto.warehouse.command.LiftSuspensionCommand;
import com.aionn.inventory.application.dto.warehouse.result.WarehouseResult;

public interface LiftWarehouseSuspensionInputPort {
    WarehouseResult execute(LiftSuspensionCommand command);
}
