package com.aionn.inventory.application.port.in.inventory;

import com.aionn.inventory.application.dto.inventory.command.AuditInventoryCommand;
import com.aionn.inventory.application.dto.inventory.result.InventoryItemResult;

public interface AuditInventoryInputPort {
    InventoryItemResult execute(AuditInventoryCommand command);
}
