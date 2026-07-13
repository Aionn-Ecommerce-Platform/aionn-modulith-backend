package com.aionn.inventory.application.port.in.inventory;

import com.aionn.inventory.application.dto.inventory.command.InitializeStockCommand;
import com.aionn.inventory.application.dto.inventory.result.InventoryItemResult;

public interface InitializeStockInputPort {
    InventoryItemResult execute(InitializeStockCommand command);
}
