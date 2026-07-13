package com.aionn.inventory.application.port.in.inventory;

import com.aionn.inventory.application.dto.inventory.command.ConfigureSafetyStockCommand;
import com.aionn.inventory.application.dto.inventory.result.InventoryItemResult;

public interface ConfigureSafetyStockInputPort {
    InventoryItemResult execute(ConfigureSafetyStockCommand command);
}
