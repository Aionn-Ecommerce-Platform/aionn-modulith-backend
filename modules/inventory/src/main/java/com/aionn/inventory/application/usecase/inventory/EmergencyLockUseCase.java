package com.aionn.inventory.application.usecase.inventory;

import com.aionn.inventory.application.dto.inventory.command.EmergencyLockCommand;
import com.aionn.inventory.application.dto.inventory.result.InventoryItemResult;
import com.aionn.inventory.application.port.in.inventory.EmergencyLockInputPort;
import com.aionn.inventory.application.service.InventoryItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmergencyLockUseCase implements EmergencyLockInputPort {

    private final InventoryItemService inventoryItemService;

    @Override
    @Transactional
    public InventoryItemResult execute(EmergencyLockCommand command) {
        return inventoryItemService.emergencyLock(command);
    }
}
