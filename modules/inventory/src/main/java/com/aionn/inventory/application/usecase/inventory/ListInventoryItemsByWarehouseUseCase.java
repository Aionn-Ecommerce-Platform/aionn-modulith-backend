package com.aionn.inventory.application.usecase.inventory;

import com.aionn.inventory.application.dto.common.PageResult;
import com.aionn.inventory.application.dto.inventory.result.InventoryItemResult;
import com.aionn.inventory.application.mapper.InventoryItemResultMapper;
import com.aionn.inventory.application.port.in.inventory.ListInventoryItemsByWarehouseInputPort;
import com.aionn.inventory.application.service.InventoryItemService;
import com.aionn.inventory.domain.model.InventoryItem;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListInventoryItemsByWarehouseUseCase implements ListInventoryItemsByWarehouseInputPort {

    private final InventoryItemService inventoryItemService;
    private final InventoryItemResultMapper inventoryItemResultMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryItemResult> execute(String ownerId, String warehouseId, Pageable pageable) {
        PageResult<InventoryItem> page = inventoryItemService.listByWarehouse(ownerId, warehouseId, pageable);
        return new PageResult<>(
                inventoryItemResultMapper.toResults(page.content()),
                page.page(),
                page.size(),
                page.returned(),
                page.totalElements());
    }
}
