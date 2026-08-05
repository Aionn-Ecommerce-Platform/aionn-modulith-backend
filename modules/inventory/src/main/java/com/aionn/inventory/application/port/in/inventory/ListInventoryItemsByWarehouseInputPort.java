package com.aionn.inventory.application.port.in.inventory;

import com.aionn.inventory.application.dto.common.PageResult;
import com.aionn.inventory.application.dto.inventory.result.InventoryItemResult;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;

public interface ListInventoryItemsByWarehouseInputPort {
    PageResult<InventoryItemResult> execute(String ownerId, String warehouseId, OffsetPagination pagination);
}
