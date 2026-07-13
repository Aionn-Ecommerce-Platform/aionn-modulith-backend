package com.aionn.inventory.application.port.in.inventory;

import com.aionn.inventory.application.dto.inventory.result.InventoryItemResult;
import java.util.List;

public interface ListInventoryItemsBySkuInputPort {
    List<InventoryItemResult> execute(String skuId);
}
