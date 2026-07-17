package com.aionn.inventory.application.usecase.inventory;

import com.aionn.inventory.application.dto.inventory.result.InventoryItemResult;
import com.aionn.inventory.application.port.in.inventory.ListInventoryItemsBySkuInputPort;
import com.aionn.inventory.application.service.InventoryItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListInventoryItemsBySkuUseCase implements ListInventoryItemsBySkuInputPort {

    private final InventoryItemService inventoryItemService;

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItemResult> execute(String skuId) {
        return inventoryItemService.listBySku(skuId);
    }
}
