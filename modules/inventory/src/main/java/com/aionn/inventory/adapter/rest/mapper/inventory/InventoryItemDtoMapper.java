package com.aionn.inventory.adapter.rest.mapper.inventory;

import com.aionn.inventory.adapter.rest.dto.inventory.AuditInventoryRequest;
import com.aionn.inventory.adapter.rest.dto.inventory.ConfigureSafetyStockRequest;
import com.aionn.inventory.adapter.rest.dto.inventory.EmergencyLockRequest;
import com.aionn.inventory.adapter.rest.dto.inventory.InitializeStockRequest;
import com.aionn.inventory.adapter.rest.dto.inventory.ManualAdjustmentRequest;
import com.aionn.inventory.adapter.rest.dto.inventory.TrackBatchAndExpiryRequest;
import com.aionn.inventory.application.dto.inventory.command.AuditInventoryCommand;
import com.aionn.inventory.application.dto.inventory.command.ConfigureSafetyStockCommand;
import com.aionn.inventory.application.dto.inventory.command.EmergencyLockCommand;
import com.aionn.inventory.application.dto.inventory.command.InitializeStockCommand;
import com.aionn.inventory.application.dto.inventory.command.ManualAdjustmentCommand;
import com.aionn.inventory.application.dto.inventory.command.TrackBatchAndExpiryCommand;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InventoryItemDtoMapper {

    InitializeStockCommand toInitializeStockCommand(String ownerId, InitializeStockRequest request);

    ConfigureSafetyStockCommand toConfigureSafetyStockCommand(String ownerId, String skuId, String warehouseId, ConfigureSafetyStockRequest request);

    TrackBatchAndExpiryCommand toTrackBatchAndExpiryCommand(String ownerId, String skuId, String warehouseId, TrackBatchAndExpiryRequest request);

    ManualAdjustmentCommand toManualAdjustmentCommand(String ownerId, String skuId, String warehouseId, ManualAdjustmentRequest request);

    EmergencyLockCommand toEmergencyLockCommand(String adminId, String skuId, String warehouseId, EmergencyLockRequest request);

    AuditInventoryCommand toAuditInventoryCommand(String ownerId, String skuId, String warehouseId, AuditInventoryRequest request);
}
