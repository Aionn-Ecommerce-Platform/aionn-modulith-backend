package com.aionn.inventory.adapter.rest.controller;

import com.aionn.inventory.adapter.rest.dto.reservation.request.ReleaseReservationRequest;
import com.aionn.inventory.adapter.rest.dto.reservation.request.ReserveStockRequest;
import com.aionn.inventory.adapter.rest.dto.reservation.response.ReservationResponse;
import com.aionn.inventory.adapter.rest.mapper.reservation.StockReservationDtoMapper;
import com.aionn.inventory.application.dto.reservation.command.CommitReservationCommand;
import com.aionn.inventory.application.dto.reservation.result.ReservationResult;
import com.aionn.inventory.application.port.in.reservation.CommitReservationInputPort;
import com.aionn.inventory.application.port.in.reservation.GetReservationInputPort;
import com.aionn.inventory.application.port.in.reservation.ReleaseReservationInputPort;
import com.aionn.inventory.application.port.in.reservation.ReserveStockInputPort;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory/reservations")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
@Tag(name = "Inventory - Reservation", description = "System-level stock reservation lifecycle")
public class StockReservationController {

    private final ReserveStockInputPort reserveStockInputPort;
    private final CommitReservationInputPort commitReservationInputPort;
    private final ReleaseReservationInputPort releaseReservationInputPort;
    private final GetReservationInputPort getReservationInputPort;

    private final StockReservationDtoMapper dtoMapper;

    @PostMapping
    @Operation(summary = "Reserve stock")
    public ResponseEntity<ApiResponse<ReservationResponse>> reserve(@Valid @RequestBody ReserveStockRequest request) {
        ReservationResult result = reserveStockInputPort.execute(dtoMapper.toReserveStockCommand(request));
        return ApiResponse.createdResponse("Reservation processed", dtoMapper.toResponse(result));
    }

    @PostMapping("/{reservationId}/commit")
    @Operation(summary = "Commit reservation")
    public ResponseEntity<ApiResponse<ReservationResponse>> commit(@PathVariable String reservationId) {
        ReservationResult result = commitReservationInputPort.execute(new CommitReservationCommand(reservationId));
        return ResponseEntity.ok(ApiResponse.success(dtoMapper.toResponse(result), "Reservation committed"));
    }

    @PostMapping("/{reservationId}/release")
    @Operation(summary = "Release reservation")
    public ResponseEntity<ApiResponse<ReservationResponse>> release(
            @PathVariable String reservationId,
            @Valid @RequestBody ReleaseReservationRequest request) {
        ReservationResult result = releaseReservationInputPort.execute(
                dtoMapper.toReleaseReservationCommand(reservationId, request));
        return ResponseEntity.ok(ApiResponse.success(dtoMapper.toResponse(result), "Reservation released"));
    }

    @GetMapping("/{reservationId}")
    @Operation(summary = "Get reservation")
    public ResponseEntity<ApiResponse<ReservationResponse>> get(@PathVariable String reservationId) {
        return ResponseEntity.ok(ApiResponse.success(dtoMapper.toResponse(getReservationInputPort.execute(reservationId)), "Reservation fetched"));
    }
}
