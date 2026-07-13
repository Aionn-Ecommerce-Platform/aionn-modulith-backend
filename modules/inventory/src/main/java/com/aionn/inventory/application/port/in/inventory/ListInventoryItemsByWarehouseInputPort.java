package com.aionn.inventory.application.port.in.inventory;

import com.aionn.inventory.application.dto.common.PageResult;
import com.aionn.inventory.application.dto.inventory.result.InventoryItemResult;
import org.springframework.data.domain.Pageable;

public interface ListInventoryItemsByWarehouseInputPort {
    PageResult<InventoryItemResult> execute(String ownerId, String warehouseId, Pageable pageable);
}
