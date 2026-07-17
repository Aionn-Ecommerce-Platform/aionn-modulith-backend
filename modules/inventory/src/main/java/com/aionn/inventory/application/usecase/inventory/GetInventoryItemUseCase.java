package com.aionn.inventory.application.usecase.inventory;

import com.aionn.inventory.application.dto.inventory.result.InventoryItemResult;
import com.aionn.inventory.application.port.in.inventory.GetInventoryItemInputPort;
import com.aionn.inventory.application.service.InventoryItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetInventoryItemUseCase implements GetInventoryItemInputPort {

    private final InventoryItemService inventoryItemService;

    @Override
    @Transactional(readOnly = true)
    public InventoryItemResult execute(String skuId, String warehouseId) {
        return inventoryItemService.get(skuId, warehouseId);
    }
}
