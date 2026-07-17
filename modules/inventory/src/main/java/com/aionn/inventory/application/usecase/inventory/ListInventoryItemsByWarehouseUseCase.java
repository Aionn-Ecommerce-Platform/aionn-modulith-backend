package com.aionn.inventory.application.usecase.inventory;

import com.aionn.inventory.application.dto.common.PageResult;
import com.aionn.inventory.application.dto.inventory.result.InventoryItemResult;
import com.aionn.inventory.application.port.in.inventory.ListInventoryItemsByWarehouseInputPort;
import com.aionn.inventory.application.service.InventoryItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListInventoryItemsByWarehouseUseCase implements ListInventoryItemsByWarehouseInputPort {

    private final InventoryItemService inventoryItemService;

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryItemResult> execute(String ownerId, String warehouseId, Pageable pageable) {
        return inventoryItemService.listByWarehouse(ownerId, warehouseId, pageable);
    }
}
