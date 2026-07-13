package com.aionn.inventory.adapter.rest.controller;

import com.aionn.inventory.adapter.rest.dto.reservation.ReleaseReservationRequest;
import com.aionn.inventory.adapter.rest.dto.reservation.ReserveStockRequest;
import com.aionn.inventory.application.dto.reservation.command.CommitReservationCommand;
import com.aionn.inventory.application.dto.reservation.command.ReleaseReservationCommand;
import com.aionn.inventory.application.dto.reservation.command.ReserveStockCommand;
import com.aionn.inventory.application.dto.reservation.result.ReservationResult;
import com.aionn.inventory.application.service.StockReservationService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory/reservations")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
@Tag(name = "Inventory - Reservation", description = "System-level stock reservation lifecycle")
public class StockReservationController {

    private final StockReservationService reservationService;

    @PostMapping
    @Operation(summary = "Reserve stock")
    public ResponseEntity<ApiResponse<ReservationResult>> reserve(@Valid @RequestBody ReserveStockRequest request) {
        ReservationResult result = reservationService.reserve(new ReserveStockCommand(
                request.skuId(), request.warehouseId(), request.orderId(), request.qty(), request.ttlSeconds()));
        return ApiResponse.createdResponse("Reservation processed", result);
    }

    @PostMapping("/{reservationId}/commit")
    @Operation(summary = "Commit reservation")
    public ResponseEntity<ApiResponse<ReservationResult>> commit(@PathVariable String reservationId) {
        ReservationResult result = reservationService.commit(new CommitReservationCommand(reservationId));
        return ResponseEntity.ok(ApiResponse.success(result, "Reservation committed"));
    }

    @PostMapping("/{reservationId}/release")
    @Operation(summary = "Release reservation")
    public ResponseEntity<ApiResponse<ReservationResult>> release(
            @PathVariable String reservationId,
            @Valid @RequestBody ReleaseReservationRequest request) {
        ReservationResult result = reservationService.release(
                new ReleaseReservationCommand(reservationId, request.reason()));
        return ResponseEntity.ok(ApiResponse.success(result, "Reservation released"));
    }

    @GetMapping("/{reservationId}")
    @Operation(summary = "Get reservation")
    public ResponseEntity<ApiResponse<ReservationResult>> get(@PathVariable String reservationId) {
        return ResponseEntity.ok(ApiResponse.success(reservationService.get(reservationId), "Reservation fetched"));
    }
}
