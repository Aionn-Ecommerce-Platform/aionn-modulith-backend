package com.aionn.inventory.application.usecase.inventory;

import com.aionn.inventory.application.dto.inventory.command.AuditInventoryCommand;
import com.aionn.inventory.application.dto.inventory.result.InventoryItemResult;
import com.aionn.inventory.application.port.in.inventory.AuditInventoryInputPort;
import com.aionn.inventory.application.service.InventoryItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditInventoryUseCase implements AuditInventoryInputPort {

    private final InventoryItemService inventoryItemService;

    @Override
    @Transactional
    public InventoryItemResult execute(AuditInventoryCommand command) {
        return inventoryItemService.auditInventory(command);
    }
}
