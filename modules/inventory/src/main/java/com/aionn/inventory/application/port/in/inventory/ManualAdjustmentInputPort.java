package com.aionn.inventory.application.port.in.inventory;

import com.aionn.inventory.application.dto.inventory.command.ManualAdjustmentCommand;
import com.aionn.inventory.application.dto.inventory.result.InventoryItemResult;

public interface ManualAdjustmentInputPort {
    InventoryItemResult execute(ManualAdjustmentCommand command);
}
