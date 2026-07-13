package com.aionn.inventory.application.usecase.inventory;

import com.aionn.inventory.application.dto.inventory.command.AuditInventoryCommand;
import com.aionn.inventory.application.dto.inventory.command.ConfigureSafetyStockCommand;
import com.aionn.inventory.application.dto.inventory.command.EmergencyLockCommand;
import com.aionn.inventory.application.dto.inventory.command.EmergencyUnlockCommand;
import com.aionn.inventory.application.dto.inventory.command.InitializeStockCommand;
import com.aionn.inventory.application.dto.inventory.command.ManualAdjustmentCommand;
import com.aionn.inventory.application.dto.inventory.command.TrackBatchAndExpiryCommand;
import com.aionn.inventory.application.dto.common.PageResult;
import com.aionn.inventory.application.dto.inventory.result.InventoryItemResult;
import com.aionn.inventory.application.port.in.inventory.*;
import com.aionn.inventory.application.service.InventoryItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryItemUseCase implements
        InitializeStockInputPort,
        ConfigureSafetyStockInputPort,
        ManualAdjustmentInputPort,
        EmergencyLockInputPort,
        EmergencyUnlockInputPort,
        AuditInventoryInputPort,
        TrackBatchAndExpiryInputPort,
        ListInventoryItemsBySkuInputPort,
        ListInventoryItemsByWarehouseInputPort,
        GetInventoryItemInputPort {

    private final InventoryItemService service;

    @Override
    public InventoryItemResult execute(InitializeStockCommand command) {
        return service.initialize(command);
    }

    @Override
    public InventoryItemResult execute(ConfigureSafetyStockCommand command) {
        return service.configureSafetyStock(command);
    }

    @Override
    public InventoryItemResult execute(ManualAdjustmentCommand command) {
        return service.manualAdjustment(command);
    }

    @Override
    public InventoryItemResult execute(EmergencyLockCommand command) {
        return service.emergencyLock(command);
    }

    @Override
    public InventoryItemResult execute(EmergencyUnlockCommand command) {
        return service.emergencyUnlock(command);
    }

    @Override
    public InventoryItemResult execute(AuditInventoryCommand command) {
        return service.auditInventory(command);
    }

    @Override
    public InventoryItemResult execute(TrackBatchAndExpiryCommand command) {
        return service.trackBatchAndExpiry(command);
    }

    @Override
    public List<InventoryItemResult> execute(String skuId) {
        return service.listBySku(skuId);
    }

    @Override
    public PageResult<InventoryItemResult> execute(String ownerId, String warehouseId, Pageable pageable) {
        return service.listByWarehouse(ownerId, warehouseId, pageable);
    }

    @Override
    public InventoryItemResult execute(String skuId, String warehouseId) {
        return service.get(skuId, warehouseId);
    }
}
