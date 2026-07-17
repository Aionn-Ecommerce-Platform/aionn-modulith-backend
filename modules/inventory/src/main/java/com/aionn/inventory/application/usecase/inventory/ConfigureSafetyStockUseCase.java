package com.aionn.inventory.application.usecase.inventory;

import com.aionn.inventory.application.dto.inventory.command.ConfigureSafetyStockCommand;
import com.aionn.inventory.application.dto.inventory.result.InventoryItemResult;
import com.aionn.inventory.application.port.in.inventory.ConfigureSafetyStockInputPort;
import com.aionn.inventory.application.service.InventoryItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConfigureSafetyStockUseCase implements ConfigureSafetyStockInputPort {

    private final InventoryItemService inventoryItemService;

    @Override
    @Transactional
    public InventoryItemResult execute(ConfigureSafetyStockCommand command) {
        return inventoryItemService.configureSafetyStock(command);
    }
}
