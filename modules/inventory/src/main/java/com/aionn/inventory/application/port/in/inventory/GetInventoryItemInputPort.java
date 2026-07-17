package com.aionn.inventory.application.port.in.inventory;

import com.aionn.inventory.application.dto.inventory.result.InventoryItemResult;

public interface GetInventoryItemInputPort {
    InventoryItemResult execute(String skuId, String warehouseId);
}
