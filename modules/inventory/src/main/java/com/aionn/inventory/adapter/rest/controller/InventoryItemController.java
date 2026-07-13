package com.aionn.inventory.adapter.rest.controller;

import com.aionn.inventory.adapter.rest.dto.inventory.AuditInventoryRequest;
import com.aionn.inventory.adapter.rest.dto.inventory.ConfigureSafetyStockRequest;
import com.aionn.inventory.adapter.rest.dto.inventory.EmergencyLockRequest;
import com.aionn.inventory.adapter.rest.dto.inventory.InitializeStockRequest;
import com.aionn.inventory.adapter.rest.dto.inventory.ManualAdjustmentRequest;
import com.aionn.inventory.adapter.rest.dto.inventory.TrackBatchAndExpiryRequest;
import com.aionn.inventory.adapter.rest.support.session.CurrentAdminId;
import com.aionn.inventory.application.dto.analytics.result.LowStockAlertResult;
import com.aionn.inventory.application.dto.common.PageResult;
import com.aionn.inventory.application.dto.inventory.command.AuditInventoryCommand;
import com.aionn.inventory.application.dto.inventory.command.ConfigureSafetyStockCommand;
import com.aionn.inventory.application.dto.inventory.command.EmergencyLockCommand;
import com.aionn.inventory.application.dto.inventory.command.EmergencyUnlockCommand;
import com.aionn.inventory.application.dto.inventory.command.InitializeStockCommand;
import com.aionn.inventory.application.dto.inventory.command.ManualAdjustmentCommand;
import com.aionn.inventory.application.dto.inventory.command.TrackBatchAndExpiryCommand;
import com.aionn.inventory.application.dto.inventory.result.InventoryItemResult;
import com.aionn.inventory.application.service.InventoryAnalyticsService;
import com.aionn.inventory.application.service.InventoryItemService;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/items")
@RequiredArgsConstructor
@Tag(name = "Inventory - Item", description = "InventoryItem operations: initialize, safety stock, audit, lock")
public class InventoryItemController {

        private final InventoryItemService service;
        private final InventoryAnalyticsService analyticsService;

        @GetMapping("/merchant/low-stock")
        @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
        @Operation(summary = "Low-stock alerts for the authenticated merchant")
        public ResponseEntity<ApiResponse<List<LowStockAlertResult>>> merchantLowStock(
                        Authentication authentication) {
                return ResponseEntity.ok(ApiResponse.success(
                                analyticsService.getMerchantLowStock(authentication.getName()),
                                "Low-stock alerts fetched"));
        }

        @PostMapping
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Initialize stock")
        public ResponseEntity<ApiResponse<InventoryItemResult>> initialize(
                        Authentication authentication,
                        @Valid @RequestBody InitializeStockRequest request) {
                InventoryItemResult result = service.initialize(new InitializeStockCommand(
                                authentication.getName(), request.skuId(), request.warehouseId(),
                                request.initialQty()));
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
                InventoryItemResult result = service.configureSafetyStock(new ConfigureSafetyStockCommand(
                                authentication.getName(), skuId, warehouseId, request.safetyStockQty()));
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
                InventoryItemResult result = service.trackBatchAndExpiry(new TrackBatchAndExpiryCommand(
                                authentication.getName(), skuId, warehouseId, request.batchNo(), request.expiryDate()));
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
                InventoryItemResult result = service.manualAdjustment(new ManualAdjustmentCommand(
                                authentication.getName(), skuId, warehouseId, request.qty(), request.type(),
                                request.reason()));
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
                InventoryItemResult result = service.emergencyLock(new EmergencyLockCommand(
                                adminId, skuId, warehouseId, request.reason()));
                return ResponseEntity.ok(ApiResponse.success(result, "Inventory item locked"));
        }

        @PostMapping("/{skuId}/{warehouseId}/unlock")
        @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
        @Operation(summary = "Emergency unlock", description = "Lifts an emergency lock")
        public ResponseEntity<ApiResponse<InventoryItemResult>> emergencyUnlock(
                        @CurrentAdminId String adminId,
                        @PathVariable String skuId,
                        @PathVariable String warehouseId) {
                InventoryItemResult result = service.emergencyUnlock(new EmergencyUnlockCommand(
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
                InventoryItemResult result = service.auditInventory(new AuditInventoryCommand(
                                authentication.getName(), skuId, warehouseId, request.actualQty()));
                return ResponseEntity.ok(ApiResponse.success(result, "Audit recorded"));
        }

        @GetMapping
        @Operation(summary = "List inventory items by SKU", description = "Public read for product detail stock summary")
        public ResponseEntity<ApiResponse<java.util.List<InventoryItemResult>>> listBySku(
                        @org.springframework.web.bind.annotation.RequestParam("skuId") String skuId) {
                return ResponseEntity.ok(
                                ApiResponse.success(service.listBySku(skuId), "Inventory items fetched"));
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
                PageResult<InventoryItemResult> result = service.listByWarehouse(
                                authentication.getName(), warehouseId, PageRequest.of(page, size));
                return ResponseEntity.ok(ApiResponse.success(result, "Inventory items fetched"));
        }

        @GetMapping("/{skuId}/{warehouseId}")
        @PreAuthorize("hasAnyAuthority('ROLE_MERCHANT','ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
        @Operation(summary = "Get inventory item")
        public ResponseEntity<ApiResponse<InventoryItemResult>> get(
                        @PathVariable String skuId,
                        @PathVariable String warehouseId) {
                return ResponseEntity
                                .ok(ApiResponse.success(service.get(skuId, warehouseId), "Inventory item fetched"));
        }
}
