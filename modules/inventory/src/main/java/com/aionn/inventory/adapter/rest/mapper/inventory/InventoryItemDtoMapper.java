package com.aionn.inventory.adapter.rest.mapper.inventory;

import com.aionn.inventory.adapter.rest.dto.inventory.request.AuditInventoryRequest;
import com.aionn.inventory.adapter.rest.dto.inventory.request.ConfigureSafetyStockRequest;
import com.aionn.inventory.adapter.rest.dto.inventory.request.EmergencyLockRequest;
import com.aionn.inventory.adapter.rest.dto.inventory.request.InitializeStockRequest;
import com.aionn.inventory.adapter.rest.dto.inventory.request.ManualAdjustmentRequest;
import com.aionn.inventory.adapter.rest.dto.inventory.request.TrackBatchAndExpiryRequest;
import com.aionn.inventory.adapter.rest.dto.inventory.response.InventoryItemResponse;
import com.aionn.inventory.adapter.rest.dto.inventory.response.LowStockAlertResponse;
import com.aionn.inventory.application.dto.analytics.result.LowStockAlertResult;
import com.aionn.inventory.application.dto.common.PageResult;
import com.aionn.inventory.application.dto.inventory.command.AuditInventoryCommand;
import com.aionn.inventory.application.dto.inventory.command.ConfigureSafetyStockCommand;
import com.aionn.inventory.application.dto.inventory.command.EmergencyLockCommand;
import com.aionn.inventory.application.dto.inventory.command.InitializeStockCommand;
import com.aionn.inventory.application.dto.inventory.command.ManualAdjustmentCommand;
import com.aionn.inventory.application.dto.inventory.command.TrackBatchAndExpiryCommand;
import com.aionn.inventory.application.dto.inventory.result.InventoryItemResult;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface InventoryItemDtoMapper {

    InitializeStockCommand toInitializeStockCommand(String ownerId, InitializeStockRequest request);

    ConfigureSafetyStockCommand toConfigureSafetyStockCommand(String ownerId, String skuId, String warehouseId, ConfigureSafetyStockRequest request);

    TrackBatchAndExpiryCommand toTrackBatchAndExpiryCommand(String ownerId, String skuId, String warehouseId, TrackBatchAndExpiryRequest request);

    ManualAdjustmentCommand toManualAdjustmentCommand(String ownerId, String skuId, String warehouseId, ManualAdjustmentRequest request);

    EmergencyLockCommand toEmergencyLockCommand(String adminId, String skuId, String warehouseId, EmergencyLockRequest request);

    AuditInventoryCommand toAuditInventoryCommand(String ownerId, String skuId, String warehouseId, AuditInventoryRequest request);

    InventoryItemResponse toResponse(InventoryItemResult result);

    List<InventoryItemResponse> toResponses(List<InventoryItemResult> results);

    LowStockAlertResponse toLowStockResponse(LowStockAlertResult result);

    List<LowStockAlertResponse> toLowStockResponses(List<LowStockAlertResult> results);

    default PageResult<InventoryItemResponse> toResponsePage(PageResult<InventoryItemResult> page) {
        return new PageResult<>(
                toResponses(page.content()),
                page.page(),
                page.size(),
                page.returned(),
                page.totalElements());
    }
}
