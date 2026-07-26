package com.aionn.promotion.adapter.rest.controller;

import com.aionn.promotion.adapter.rest.dto.flashsale.RegisterFlashSaleRequest;
import com.aionn.promotion.adapter.rest.dto.flashsale.RejectFlashSaleRequest;
import com.aionn.promotion.adapter.rest.support.session.CurrentAdminId;
import com.aionn.promotion.adapter.rest.support.session.CurrentUserId;
import com.aionn.promotion.application.dto.flashsale.command.FlashSaleCommands;
import com.aionn.promotion.application.dto.flashsale.result.ActiveFlashSaleResult;
import com.aionn.promotion.application.dto.flashsale.result.FlashSaleRegistrationResult;
import com.aionn.promotion.application.service.FlashSaleService;
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

    private final FlashSaleService flashSaleService;

    @PostMapping("/registrations")
    @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
    @Operation(summary = "Merchant registers a SKU for a flash sale slot")
    public ResponseEntity<ApiResponse<FlashSaleRegistrationResult>> register(
            @CurrentUserId String ownerId,
            @Valid @RequestBody RegisterFlashSaleRequest request) {
        return ApiResponse.createdResponse("Flash-sale registration submitted",
                flashSaleService.register(new FlashSaleCommands.RegisterFlashSale(
                        request.campaignId(), ownerId,
                        request.productId(), request.skuId(),
                        request.salePrice(), request.currency(), request.saleStock())));
    }

    @PostMapping("/registrations/{registrationId}/approve")
    @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Admin approves a pending flash-sale registration")
    public ResponseEntity<ApiResponse<FlashSaleRegistrationResult>> approve(
            @CurrentAdminId String adminId,
            @PathVariable String registrationId) {
        return ResponseEntity.ok(ApiResponse.success(
                flashSaleService.approve(new FlashSaleCommands.ApproveFlashSale(
                        registrationId, adminId)),
                "Flash-sale registration approved"));
    }

    @PostMapping("/registrations/{registrationId}/reject")
    @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Admin rejects a pending flash-sale registration")
    public ResponseEntity<ApiResponse<FlashSaleRegistrationResult>> reject(
            @CurrentAdminId String adminId,
            @PathVariable String registrationId,
            @Valid @RequestBody RejectFlashSaleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                flashSaleService.reject(new FlashSaleCommands.RejectFlashSale(
                        registrationId, adminId, request.reason())),
                "Flash-sale registration rejected"));
    }

    @DeleteMapping("/registrations/{registrationId}")
    @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
    @Operation(summary = "Merchant cancels their own pending registration")
    public ResponseEntity<ApiResponse<FlashSaleRegistrationResult>> cancel(
            @CurrentUserId String ownerId,
            @PathVariable String registrationId) {
        return ResponseEntity.ok(ApiResponse.success(
                flashSaleService.cancel(new FlashSaleCommands.CancelFlashSale(
                        registrationId, ownerId)),
                "Flash-sale registration cancelled"));
    }

    @GetMapping("/registrations/mine")
    @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
    @Operation(summary = "List my flash-sale registrations")
    public ResponseEntity<ApiResponse<List<FlashSaleRegistrationResult>>> listMine(
            @CurrentUserId String ownerId,
            @RequestParam(required = false) FlashSaleRegistrationStatus status,
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(ApiResponse.success(
                flashSaleService.listByMerchant(ownerId, status, limit),
                "Flash-sale registrations fetched"));
    }

    @GetMapping("/registrations")
    @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Admin lists registrations by status")
    public ResponseEntity<ApiResponse<List<FlashSaleRegistrationResult>>> listByStatus(
            @RequestParam FlashSaleRegistrationStatus status,
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(ApiResponse.success(
                flashSaleService.listByStatus(status, limit),
                "Flash-sale registrations fetched"));
    }

    @GetMapping("/registrations/{registrationId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get flash-sale registration by id")
    public ResponseEntity<ApiResponse<FlashSaleRegistrationResult>> get(
            @PathVariable String registrationId) {
        return ResponseEntity.ok(ApiResponse.success(
                flashSaleService.get(registrationId), "Flash-sale registration fetched"));
    }

    @GetMapping("/active")
    @Operation(summary = "Public — active flash sales for the storefront")
    public ResponseEntity<ApiResponse<List<ActiveFlashSaleResult>>> active(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(ApiResponse.success(
                flashSaleService.listActive(limit), "Active flash sales fetched"));
    }
}
