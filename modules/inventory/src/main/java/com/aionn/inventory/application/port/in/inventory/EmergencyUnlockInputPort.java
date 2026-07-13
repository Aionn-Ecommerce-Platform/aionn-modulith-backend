package com.aionn.inventory.application.port.in.inventory;

import com.aionn.inventory.application.dto.inventory.command.EmergencyUnlockCommand;
import com.aionn.inventory.application.dto.inventory.result.InventoryItemResult;

public interface EmergencyUnlockInputPort {
    InventoryItemResult execute(EmergencyUnlockCommand command);
}
