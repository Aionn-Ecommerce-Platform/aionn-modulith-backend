package com.aionn.inventory.adapter.rest.controller;

import com.aionn.inventory.adapter.rest.dto.warehouse.request.AdjustPriorityRequest;
import com.aionn.inventory.adapter.rest.dto.warehouse.request.AdminReasonRequest;
import com.aionn.inventory.adapter.rest.dto.warehouse.request.ChangeWarehouseStatusRequest;
import com.aionn.inventory.adapter.rest.dto.warehouse.request.CreateWarehouseRequest;
import com.aionn.inventory.adapter.rest.dto.warehouse.response.WarehouseResponse;
import com.aionn.inventory.adapter.rest.mapper.warehouse.WarehouseDtoMapper;
import com.aionn.inventory.adapter.rest.support.session.CurrentAdminId;
import com.aionn.inventory.adapter.rest.support.session.CurrentMerchantId;
import com.aionn.inventory.application.dto.warehouse.command.LiftSuspensionCommand;
import com.aionn.inventory.application.dto.warehouse.query.ListWarehousesByOwnerQuery;
import com.aionn.inventory.application.dto.warehouse.result.WarehouseResult;
import com.aionn.inventory.application.port.in.warehouse.AdjustWarehousePriorityInputPort;
import com.aionn.inventory.application.port.in.warehouse.ChangeWarehouseStatusInputPort;
import com.aionn.inventory.application.port.in.warehouse.CreateWarehouseInputPort;
import com.aionn.inventory.application.port.in.warehouse.GetWarehouseInputPort;
import com.aionn.inventory.application.port.in.warehouse.LiftWarehouseSuspensionInputPort;
import com.aionn.inventory.application.port.in.warehouse.ListWarehousesByOwnerInputPort;
import com.aionn.inventory.application.port.in.warehouse.SuspendWarehouseInputPort;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/warehouses")
@RequiredArgsConstructor
@Tag(name = "Inventory - Warehouse", description = "Warehouse lifecycle endpoints")
public class WarehouseController {

    private final CreateWarehouseInputPort createWarehouseInputPort;
    private final ChangeWarehouseStatusInputPort changeWarehouseStatusInputPort;
    private final AdjustWarehousePriorityInputPort adjustWarehousePriorityInputPort;
    private final SuspendWarehouseInputPort suspendWarehouseInputPort;
    private final LiftWarehouseSuspensionInputPort liftWarehouseSuspensionInputPort;
    private final GetWarehouseInputPort getWarehouseInputPort;
    private final ListWarehousesByOwnerInputPort listWarehousesByOwnerInputPort;

    private final WarehouseDtoMapper dtoMapper;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create warehouse")
    public ResponseEntity<ApiResponse<WarehouseResponse>> create(
            org.springframework.security.core.Authentication authentication,
            @CurrentMerchantId String merchantId,
            @Valid @RequestBody CreateWarehouseRequest request) {
        WarehouseResult result = createWarehouseInputPort.execute(
                dtoMapper.toCreateWarehouseCommand(authentication.getName(), request));
        return ApiResponse.createdResponse("Warehouse created", dtoMapper.toResponse(result));
    }

    @PutMapping("/{warehouseId}/status")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Change warehouse status")
    public ResponseEntity<ApiResponse<WarehouseResponse>> changeStatus(
            @CurrentMerchantId String merchantId,
            @PathVariable String warehouseId,
            @Valid @RequestBody ChangeWarehouseStatusRequest request) {
        WarehouseResult result = changeWarehouseStatusInputPort.execute(
                dtoMapper.toChangeStatusCommand(warehouseId, merchantId, request));
        return ResponseEntity.ok(ApiResponse.success(dtoMapper.toResponse(result), "Warehouse status updated"));
    }

    @PutMapping("/{warehouseId}/priority")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Adjust priority")
    public ResponseEntity<ApiResponse<WarehouseResponse>> adjustPriority(
            @CurrentMerchantId String merchantId,
            @PathVariable String warehouseId,
            @Valid @RequestBody AdjustPriorityRequest request) {
        WarehouseResult result = adjustWarehousePriorityInputPort.execute(
                dtoMapper.toAdjustPriorityCommand(warehouseId, merchantId, request));
        return ResponseEntity.ok(ApiResponse.success(dtoMapper.toResponse(result), "Warehouse priority updated"));
    }

    @PostMapping("/{warehouseId}/suspend")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
    @Operation(summary = "Suspend warehouse")
    public ResponseEntity<ApiResponse<WarehouseResponse>> suspend(
            @CurrentAdminId String adminId,
            @PathVariable String warehouseId,
            @Valid @RequestBody AdminReasonRequest request) {
        WarehouseResult result = suspendWarehouseInputPort.execute(
                dtoMapper.toSuspendWarehouseCommand(warehouseId, adminId, request));
        return ResponseEntity.ok(ApiResponse.success(dtoMapper.toResponse(result), "Warehouse suspended"));
    }

    @PostMapping("/{warehouseId}/lift-suspension")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
    @Operation(summary = "Lift suspension", description = "Admin reactivates a suspended warehouse")
    public ResponseEntity<ApiResponse<WarehouseResponse>> liftSuspension(
            @CurrentAdminId String adminId,
            @PathVariable String warehouseId) {
        WarehouseResult result = liftWarehouseSuspensionInputPort.execute(
                new LiftSuspensionCommand(warehouseId, adminId));
        return ResponseEntity.ok(ApiResponse.success(dtoMapper.toResponse(result), "Warehouse suspension lifted"));
    }

    @GetMapping("/{warehouseId}")
    @Operation(summary = "Get warehouse")
    public ResponseEntity<ApiResponse<WarehouseResponse>> get(@PathVariable String warehouseId) {
        return ResponseEntity.ok(ApiResponse.success(
                dtoMapper.toResponse(getWarehouseInputPort.execute(warehouseId)), "Warehouse fetched"));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List my warehouses", description = "Returns warehouses owned by the caller, ordered by priority")
    public ResponseEntity<ApiResponse<List<WarehouseResponse>>> listMine(
            @CurrentMerchantId String merchantId) {
        List<WarehouseResult> results = listWarehousesByOwnerInputPort.execute(
                new ListWarehousesByOwnerQuery(merchantId));
        return ResponseEntity.ok(ApiResponse.success(dtoMapper.toResponses(results), "Warehouses fetched"));
    }
}
