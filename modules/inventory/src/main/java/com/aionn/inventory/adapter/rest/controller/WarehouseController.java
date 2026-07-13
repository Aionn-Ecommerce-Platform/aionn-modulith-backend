package com.aionn.inventory.adapter.rest.controller;

import com.aionn.inventory.adapter.rest.dto.warehouse.AdjustPriorityRequest;
import com.aionn.inventory.adapter.rest.dto.warehouse.AdminReasonRequest;
import com.aionn.inventory.adapter.rest.dto.warehouse.ChangeWarehouseStatusRequest;
import com.aionn.inventory.adapter.rest.dto.warehouse.CreateWarehouseRequest;
import com.aionn.inventory.adapter.rest.mapper.warehouse.WarehouseDtoMapper;
import com.aionn.inventory.adapter.rest.support.session.CurrentAdminId;
import com.aionn.inventory.application.dto.warehouse.command.LiftSuspensionCommand;
import com.aionn.inventory.application.dto.warehouse.result.WarehouseResult;
import com.aionn.inventory.application.port.in.warehouse.*;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<ApiResponse<WarehouseResult>> create(
            Authentication authentication,
            @Valid @RequestBody CreateWarehouseRequest request) {
        WarehouseResult result = createWarehouseInputPort.execute(
                dtoMapper.toCreateWarehouseCommand(authentication.getName(), request));
        return ApiResponse.createdResponse("Warehouse created", result);
    }

    @PutMapping("/{warehouseId}/status")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Change warehouse status")
    public ResponseEntity<ApiResponse<WarehouseResult>> changeStatus(
            Authentication authentication,
            @PathVariable String warehouseId,
            @Valid @RequestBody ChangeWarehouseStatusRequest request) {
        WarehouseResult result = changeWarehouseStatusInputPort.execute(
                dtoMapper.toChangeStatusCommand(warehouseId, authentication.getName(), request));
        return ResponseEntity.ok(ApiResponse.success(result, "Warehouse status updated"));
    }

    @PutMapping("/{warehouseId}/priority")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Adjust priority")
    public ResponseEntity<ApiResponse<WarehouseResult>> adjustPriority(
            Authentication authentication,
            @PathVariable String warehouseId,
            @Valid @RequestBody AdjustPriorityRequest request) {
        WarehouseResult result = adjustWarehousePriorityInputPort.execute(
                dtoMapper.toAdjustPriorityCommand(warehouseId, authentication.getName(), request));
        return ResponseEntity.ok(ApiResponse.success(result, "Warehouse priority updated"));
    }

    @PostMapping("/{warehouseId}/suspend")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
    @Operation(summary = "Suspend warehouse")
    public ResponseEntity<ApiResponse<WarehouseResult>> suspend(
            @CurrentAdminId String adminId,
            @PathVariable String warehouseId,
            @Valid @RequestBody AdminReasonRequest request) {
        WarehouseResult result = suspendWarehouseInputPort.execute(
                dtoMapper.toSuspendWarehouseCommand(warehouseId, adminId, request));
        return ResponseEntity.ok(ApiResponse.success(result, "Warehouse suspended"));
    }

    @PostMapping("/{warehouseId}/lift-suspension")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
    @Operation(summary = "Lift suspension", description = "Admin reactivates a suspended warehouse")
    public ResponseEntity<ApiResponse<WarehouseResult>> liftSuspension(
            @CurrentAdminId String adminId,
            @PathVariable String warehouseId) {
        WarehouseResult result = liftWarehouseSuspensionInputPort.execute(
                new LiftSuspensionCommand(warehouseId, adminId));
        return ResponseEntity.ok(ApiResponse.success(result, "Warehouse suspension lifted"));
    }

    @GetMapping("/{warehouseId}")
    @Operation(summary = "Get warehouse")
    public ResponseEntity<ApiResponse<WarehouseResult>> get(@PathVariable String warehouseId) {
        return ResponseEntity.ok(ApiResponse.success(getWarehouseInputPort.execute(warehouseId), "Warehouse fetched"));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List my warehouses", description = "Returns warehouses owned by the caller, ordered by priority")
    public ResponseEntity<ApiResponse<List<WarehouseResult>>> listMine(Authentication authentication) {
        List<WarehouseResult> results = listWarehousesByOwnerInputPort.execute(
                new com.aionn.inventory.application.dto.warehouse.query.ListWarehousesByOwnerQuery(authentication.getName()));
        return ResponseEntity.ok(ApiResponse.success(results, "Warehouses fetched"));
    }
}
