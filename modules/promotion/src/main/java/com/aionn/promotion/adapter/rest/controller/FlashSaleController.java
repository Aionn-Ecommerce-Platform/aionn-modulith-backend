package com.aionn.promotion.adapter.rest.controller;

import com.aionn.promotion.adapter.rest.dto.flashsale.RegisterFlashSaleRequest;
import com.aionn.promotion.adapter.rest.dto.flashsale.RejectFlashSaleRequest;
import com.aionn.promotion.adapter.rest.dto.flashsale.response.ActiveFlashSaleResponse;
import com.aionn.promotion.adapter.rest.dto.flashsale.response.FlashSaleRegistrationResponse;
import com.aionn.promotion.adapter.rest.mapper.flashsale.FlashSaleDtoMapper;
import com.aionn.promotion.adapter.rest.support.session.CurrentAdminId;
import com.aionn.promotion.adapter.rest.support.session.CurrentUserId;
import com.aionn.promotion.application.port.in.flashsale.ApproveFlashSaleInputPort;
import com.aionn.promotion.application.port.in.flashsale.CancelFlashSaleInputPort;
import com.aionn.promotion.application.port.in.flashsale.GetFlashSaleRegistrationInputPort;
import com.aionn.promotion.application.port.in.flashsale.ListActiveFlashSalesInputPort;
import com.aionn.promotion.application.port.in.flashsale.ListFlashSaleRegistrationsByStatusInputPort;
import com.aionn.promotion.application.port.in.flashsale.ListMyFlashSaleRegistrationsInputPort;
import com.aionn.promotion.application.port.in.flashsale.RegisterFlashSaleInputPort;
import com.aionn.promotion.application.port.in.flashsale.RejectFlashSaleInputPort;
import com.aionn.promotion.domain.valueobject.FlashSaleRegistrationStatus;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/promotions/flash-sales")
@RequiredArgsConstructor
@Tag(name = "Promotion - Flash Sale", description = "Flash sale registration + admin approval")
public class FlashSaleController {

        private final RegisterFlashSaleInputPort registerFlashSaleInputPort;
        private final ApproveFlashSaleInputPort approveFlashSaleInputPort;
        private final RejectFlashSaleInputPort rejectFlashSaleInputPort;
        private final CancelFlashSaleInputPort cancelFlashSaleInputPort;
        private final ListMyFlashSaleRegistrationsInputPort listMyFlashSaleRegistrationsInputPort;
        private final ListFlashSaleRegistrationsByStatusInputPort listFlashSaleRegistrationsByStatusInputPort;
        private final GetFlashSaleRegistrationInputPort getFlashSaleRegistrationInputPort;
        private final ListActiveFlashSalesInputPort listActiveFlashSalesInputPort;
        private final FlashSaleDtoMapper dtoMapper;

        @PostMapping("/registrations")
        @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
        @Operation(summary = "Merchant registers a SKU for a flash sale slot")
        public ResponseEntity<ApiResponse<FlashSaleRegistrationResponse>> register(
                        @CurrentUserId String ownerId,
                        @Valid @RequestBody RegisterFlashSaleRequest request) {
                return ApiResponse.createdResponse("Flash-sale registration submitted",
                                dtoMapper.toResponse(registerFlashSaleInputPort.execute(
                                                dtoMapper.toRegisterCommand(ownerId, request))));
        }

        @PostMapping("/registrations/{registrationId}/approve")
        @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
        @Operation(summary = "Admin approves a pending flash-sale registration")
        public ResponseEntity<ApiResponse<FlashSaleRegistrationResponse>> approve(
                        @CurrentAdminId String adminId,
                        @PathVariable String registrationId) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponse(approveFlashSaleInputPort.execute(
                                                dtoMapper.toApproveCommand(registrationId, adminId))),
                                "Flash-sale registration approved"));
        }

        @PostMapping("/registrations/{registrationId}/reject")
        @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
        @Operation(summary = "Admin rejects a pending flash-sale registration")
        public ResponseEntity<ApiResponse<FlashSaleRegistrationResponse>> reject(
                        @CurrentAdminId String adminId,
                        @PathVariable String registrationId,
                        @Valid @RequestBody RejectFlashSaleRequest request) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponse(rejectFlashSaleInputPort.execute(
                                                dtoMapper.toRejectCommand(registrationId, adminId, request))),
                                "Flash-sale registration rejected"));
        }

        @DeleteMapping("/registrations/{registrationId}")
        @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
        @Operation(summary = "Merchant cancels their own pending registration")
        public ResponseEntity<ApiResponse<FlashSaleRegistrationResponse>> cancel(
                        @CurrentUserId String ownerId,
                        @PathVariable String registrationId) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponse(cancelFlashSaleInputPort.execute(
                                                dtoMapper.toCancelCommand(registrationId, ownerId))),
                                "Flash-sale registration cancelled"));
        }

        @GetMapping("/registrations/mine")
        @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
        @Operation(summary = "List my flash-sale registrations")
        public ResponseEntity<ApiResponse<List<FlashSaleRegistrationResponse>>> listMine(
                        @CurrentUserId String ownerId,
                        @RequestParam(required = false) FlashSaleRegistrationStatus status,
                        @RequestParam(defaultValue = "100") int limit) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponses(
                                                listMyFlashSaleRegistrationsInputPort.execute(ownerId, status, limit)),
                                "Flash-sale registrations fetched"));
        }

        @GetMapping("/registrations")
        @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
        @Operation(summary = "Admin lists registrations by status")
        public ResponseEntity<ApiResponse<List<FlashSaleRegistrationResponse>>> listByStatus(
                        @RequestParam FlashSaleRegistrationStatus status,
                        @RequestParam(defaultValue = "100") int limit) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponses(
                                                listFlashSaleRegistrationsByStatusInputPort.execute(status, limit)),
                                "Flash-sale registrations fetched"));
        }

        @GetMapping("/registrations/{registrationId}")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Get flash-sale registration by id")
        public ResponseEntity<ApiResponse<FlashSaleRegistrationResponse>> get(
                        @CurrentUserId String ownerId,
                        @PathVariable String registrationId) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponse(getFlashSaleRegistrationInputPort.execute(registrationId, ownerId)),
                                "Flash-sale registration fetched"));
        }

        @GetMapping("/active")
        @Operation(summary = "Public — active flash sales for the storefront")
        public ResponseEntity<ApiResponse<List<ActiveFlashSaleResponse>>> active(
                        @RequestParam(defaultValue = "5") int limit) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toActiveResponses(listActiveFlashSalesInputPort.execute(limit)),
                                "Active flash sales fetched"));
        }
}
