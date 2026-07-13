package com.aionn.inventory.adapter.rest.controller;

import com.aionn.inventory.adapter.rest.dto.warehouse.AdjustPriorityRequest;
import com.aionn.inventory.adapter.rest.dto.warehouse.AdminReasonRequest;
import com.aionn.inventory.adapter.rest.dto.warehouse.ChangeWarehouseStatusRequest;
import com.aionn.inventory.adapter.rest.dto.warehouse.CreateWarehouseRequest;
import com.aionn.inventory.adapter.rest.support.session.CurrentAdminId;
import com.aionn.inventory.application.dto.warehouse.command.AdjustPriorityCommand;
import com.aionn.inventory.application.dto.warehouse.command.ChangeStatusCommand;
import com.aionn.inventory.application.dto.warehouse.command.CreateWarehouseCommand;
import com.aionn.inventory.application.dto.warehouse.command.LiftSuspensionCommand;
import com.aionn.inventory.application.dto.warehouse.command.SuspendWarehouseCommand;
import com.aionn.inventory.application.dto.warehouse.result.WarehouseResult;
import com.aionn.inventory.application.service.WarehouseService;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

    private final WarehouseService warehouseService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create warehouse")
    public ResponseEntity<ApiResponse<WarehouseResult>> create(
            Authentication authentication,
            @Valid @RequestBody CreateWarehouseRequest request) {
        WarehouseResult result = warehouseService.create(new CreateWarehouseCommand(
                authentication.getName(), request.address(), request.priorityLevel()));
        return ApiResponse.createdResponse("Warehouse created", result);
    }

    @PutMapping("/{warehouseId}/status")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Change warehouse status")
    public ResponseEntity<ApiResponse<WarehouseResult>> changeStatus(
            Authentication authentication,
            @PathVariable String warehouseId,
            @Valid @RequestBody ChangeWarehouseStatusRequest request) {
        WarehouseResult result = warehouseService.changeStatus(new ChangeStatusCommand(
                warehouseId, authentication.getName(), request.status()));
        return ResponseEntity.ok(ApiResponse.success(result, "Warehouse status updated"));
    }

    @PutMapping("/{warehouseId}/priority")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Adjust priority")
    public ResponseEntity<ApiResponse<WarehouseResult>> adjustPriority(
            Authentication authentication,
            @PathVariable String warehouseId,
            @Valid @RequestBody AdjustPriorityRequest request) {
        WarehouseResult result = warehouseService.adjustPriority(new AdjustPriorityCommand(
                warehouseId, authentication.getName(), request.priorityLevel()));
        return ResponseEntity.ok(ApiResponse.success(result, "Warehouse priority updated"));
    }

    @PostMapping("/{warehouseId}/suspend")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
    @Operation(summary = "Suspend warehouse")
    public ResponseEntity<ApiResponse<WarehouseResult>> suspend(
            @CurrentAdminId String adminId,
            @PathVariable String warehouseId,
            @Valid @RequestBody AdminReasonRequest request) {
        WarehouseResult result = warehouseService.suspend(new SuspendWarehouseCommand(
                warehouseId, adminId, request.reason()));
        return ResponseEntity.ok(ApiResponse.success(result, "Warehouse suspended"));
    }

    @PostMapping("/{warehouseId}/lift-suspension")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
    @Operation(summary = "Lift suspension", description = "Admin reactivates a suspended warehouse")
    public ResponseEntity<ApiResponse<WarehouseResult>> liftSuspension(
            @CurrentAdminId String adminId,
            @PathVariable String warehouseId) {
        WarehouseResult result = warehouseService.liftSuspension(new LiftSuspensionCommand(
                warehouseId, adminId));
        return ResponseEntity.ok(ApiResponse.success(result, "Warehouse suspension lifted"));
    }

    @GetMapping("/{warehouseId}")
    @Operation(summary = "Get warehouse")
    public ResponseEntity<ApiResponse<WarehouseResult>> get(@PathVariable String warehouseId) {
        return ResponseEntity.ok(ApiResponse.success(warehouseService.get(warehouseId), "Warehouse fetched"));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List my warehouses", description = "Returns warehouses owned by the caller, ordered by priority")
    public ResponseEntity<ApiResponse<List<WarehouseResult>>> listMine(Authentication authentication) {
        List<WarehouseResult> results = warehouseService.listByOwner(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(results, "Warehouses fetched"));
    }
}
