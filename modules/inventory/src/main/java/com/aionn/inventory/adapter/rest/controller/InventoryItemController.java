package com.aionn.inventory.adapter.rest.controller;

import com.aionn.inventory.adapter.rest.dto.inventory.AuditInventoryRequest;
import com.aionn.inventory.adapter.rest.dto.inventory.ConfigureSafetyStockRequest;
import com.aionn.inventory.adapter.rest.dto.inventory.EmergencyLockRequest;
import com.aionn.inventory.adapter.rest.dto.inventory.InitializeStockRequest;
import com.aionn.inventory.adapter.rest.dto.inventory.ManualAdjustmentRequest;
import com.aionn.inventory.adapter.rest.dto.inventory.TrackBatchAndExpiryRequest;
import com.aionn.inventory.adapter.rest.mapper.inventory.InventoryItemDtoMapper;
import com.aionn.inventory.adapter.rest.support.session.CurrentAdminId;
import com.aionn.inventory.application.dto.analytics.result.LowStockAlertResult;
import com.aionn.inventory.application.dto.common.PageResult;
import com.aionn.inventory.application.dto.inventory.command.EmergencyUnlockCommand;
import com.aionn.inventory.application.dto.inventory.result.InventoryItemResult;
import com.aionn.inventory.application.port.in.inventory.*;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/items")
@RequiredArgsConstructor
@Tag(name = "Inventory - Item", description = "InventoryItem operations: initialize, safety stock, audit, lock")
public class InventoryItemController {

    private final InitializeStockInputPort initializeStockInputPort;
    private final ConfigureSafetyStockInputPort configureSafetyStockInputPort;
    private final ManualAdjustmentInputPort manualAdjustmentInputPort;
    private final EmergencyLockInputPort emergencyLockInputPort;
    private final EmergencyUnlockInputPort emergencyUnlockInputPort;
    private final AuditInventoryInputPort auditInventoryInputPort;
    private final TrackBatchAndExpiryInputPort trackBatchAndExpiryInputPort;
    private final ListInventoryItemsBySkuInputPort listInventoryItemsBySkuInputPort;
    private final ListInventoryItemsByWarehouseInputPort listInventoryItemsByWarehouseInputPort;
    private final GetInventoryItemInputPort getInventoryItemInputPort;
    private final GetMerchantLowStockInputPort getMerchantLowStockInputPort;

    private final InventoryItemDtoMapper dtoMapper;

    @GetMapping("/merchant/low-stock")
    @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
    @Operation(summary = "Low-stock alerts for the authenticated merchant")
    public ResponseEntity<ApiResponse<List<LowStockAlertResult>>> merchantLowStock(
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                getMerchantLowStockInputPort.execute(authentication.getName()),
                "Low-stock alerts fetched"));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Initialize stock")
    public ResponseEntity<ApiResponse<InventoryItemResult>> initialize(
            Authentication authentication,
            @Valid @RequestBody InitializeStockRequest request) {
        InventoryItemResult result = initializeStockInputPort.execute(
                dtoMapper.toInitializeStockCommand(authentication.getName(), request));
        return ApiResponse.createdResponse("Inventory initialized", result);
    }

    @PutMapping("/{skuId}/{warehouseId}/safety-stock")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Configure safety stock")
    public ResponseEntity<ApiResponse<InventoryItemResult>> configureSafetyStock(
            Authentication authentication,
            @PathVariable String skuId,
            @PathVariable String warehouseId,
            @Valid @RequestBody ConfigureSafetyStockRequest request) {
        InventoryItemResult result = configureSafetyStockInputPort.execute(
                dtoMapper.toConfigureSafetyStockCommand(authentication.getName(), skuId, warehouseId, request));
        return ResponseEntity.ok(ApiResponse.success(result, "Safety stock configured"));
    }

    @PutMapping("/{skuId}/{warehouseId}/batch-expiry")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Track batch and expiry")
    public ResponseEntity<ApiResponse<InventoryItemResult>> trackBatchAndExpiry(
            Authentication authentication,
            @PathVariable String skuId,
            @PathVariable String warehouseId,
            @Valid @RequestBody TrackBatchAndExpiryRequest request) {
        InventoryItemResult result = trackBatchAndExpiryInputPort.execute(
                dtoMapper.toTrackBatchAndExpiryCommand(authentication.getName(), skuId, warehouseId, request));
        return ResponseEntity.ok(ApiResponse.success(result, "Batch and expiry tracked"));
    }

    @PostMapping("/{skuId}/{warehouseId}/manual-adjustment")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Manual adjustment")
    public ResponseEntity<ApiResponse<InventoryItemResult>> manualAdjustment(
            Authentication authentication,
            @PathVariable String skuId,
            @PathVariable String warehouseId,
            @Valid @RequestBody ManualAdjustmentRequest request) {
        InventoryItemResult result = manualAdjustmentInputPort.execute(
                dtoMapper.toManualAdjustmentCommand(authentication.getName(), skuId, warehouseId, request));
        return ResponseEntity.ok(ApiResponse.success(result, "Adjustment recorded"));
    }

    @PostMapping("/{skuId}/{warehouseId}/lock")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
    @Operation(summary = "Emergency lock")
    public ResponseEntity<ApiResponse<InventoryItemResult>> emergencyLock(
            @CurrentAdminId String adminId,
            @PathVariable String skuId,
            @PathVariable String warehouseId,
            @Valid @RequestBody EmergencyLockRequest request) {
        InventoryItemResult result = emergencyLockInputPort.execute(
                dtoMapper.toEmergencyLockCommand(adminId, skuId, warehouseId, request));
        return ResponseEntity.ok(ApiResponse.success(result, "Inventory item locked"));
    }

    @PostMapping("/{skuId}/{warehouseId}/unlock")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
    @Operation(summary = "Emergency unlock", description = "Lifts an emergency lock")
    public ResponseEntity<ApiResponse<InventoryItemResult>> emergencyUnlock(
            @CurrentAdminId String adminId,
            @PathVariable String skuId,
            @PathVariable String warehouseId) {
        InventoryItemResult result = emergencyUnlockInputPort.execute(new EmergencyUnlockCommand(
                adminId, skuId, warehouseId));
        return ResponseEntity.ok(ApiResponse.success(result, "Inventory item unlocked"));
    }

    @PostMapping("/{skuId}/{warehouseId}/audit")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Record audit", description = "Reconcile system count with physical count")
    public ResponseEntity<ApiResponse<InventoryItemResult>> auditInventory(
            Authentication authentication,
            @PathVariable String skuId,
            @PathVariable String warehouseId,
            @Valid @RequestBody AuditInventoryRequest request) {
        InventoryItemResult result = auditInventoryInputPort.execute(
                dtoMapper.toAuditInventoryCommand(authentication.getName(), skuId, warehouseId, request));
        return ResponseEntity.ok(ApiResponse.success(result, "Audit recorded"));
    }

    @GetMapping
    @Operation(summary = "List inventory items by SKU", description = "Public read for product detail stock summary")
    public ResponseEntity<ApiResponse<List<InventoryItemResult>>> listBySku(
            @RequestParam("skuId") String skuId) {
        return ResponseEntity.ok(
                ApiResponse.success(listInventoryItemsBySkuInputPort.execute(skuId), "Inventory items fetched"));
    }

    @GetMapping("/by-warehouse/{warehouseId}")
    @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
    @Operation(summary = "List inventory items in a warehouse",
            description = "Paginated list of inventory rows for the given warehouse. "
                    + "Only the warehouse owner can read its stock.")
    public ResponseEntity<ApiResponse<PageResult<InventoryItemResult>>> listByWarehouse(
            Authentication authentication,
            @PathVariable String warehouseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        PageResult<InventoryItemResult> result = listInventoryItemsByWarehouseInputPort.execute(
                authentication.getName(), warehouseId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result, "Inventory items fetched"));
    }

    @GetMapping("/{skuId}/{warehouseId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MERCHANT','ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
    @Operation(summary = "Get inventory item")
    public ResponseEntity<ApiResponse<InventoryItemResult>> get(
            @PathVariable String skuId,
            @PathVariable String warehouseId) {
        return ResponseEntity.ok(ApiResponse.success(
                getInventoryItemInputPort.execute(skuId, warehouseId), "Inventory item fetched"));
    }
}
