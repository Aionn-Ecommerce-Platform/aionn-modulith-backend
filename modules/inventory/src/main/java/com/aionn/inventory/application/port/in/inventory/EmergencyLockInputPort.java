package com.aionn.inventory.application.port.in.inventory;

import com.aionn.inventory.application.dto.inventory.command.EmergencyLockCommand;
import com.aionn.inventory.application.dto.inventory.result.InventoryItemResult;

public interface EmergencyLockInputPort {
    InventoryItemResult execute(EmergencyLockCommand command);
}
