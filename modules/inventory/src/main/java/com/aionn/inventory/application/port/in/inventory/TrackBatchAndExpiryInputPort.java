package com.aionn.inventory.application.port.in.inventory;

import com.aionn.inventory.application.dto.inventory.command.TrackBatchAndExpiryCommand;
import com.aionn.inventory.application.dto.inventory.result.InventoryItemResult;

public interface TrackBatchAndExpiryInputPort {
    InventoryItemResult execute(TrackBatchAndExpiryCommand command);
}
