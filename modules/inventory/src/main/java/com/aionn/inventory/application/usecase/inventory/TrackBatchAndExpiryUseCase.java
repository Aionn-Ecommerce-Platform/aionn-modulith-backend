package com.aionn.inventory.application.usecase.inventory;

import com.aionn.inventory.application.dto.inventory.command.TrackBatchAndExpiryCommand;
import com.aionn.inventory.application.dto.inventory.result.InventoryItemResult;
import com.aionn.inventory.application.port.in.inventory.TrackBatchAndExpiryInputPort;
import com.aionn.inventory.application.service.InventoryItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrackBatchAndExpiryUseCase implements TrackBatchAndExpiryInputPort {

    private final InventoryItemService inventoryItemService;

    @Override
    @Transactional
    public InventoryItemResult execute(TrackBatchAndExpiryCommand command) {
        return inventoryItemService.trackBatchAndExpiry(command);
    }
}
