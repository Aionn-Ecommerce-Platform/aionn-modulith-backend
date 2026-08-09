package com.aionn.promotion.adapter.rest.controller;

import com.aionn.promotion.adapter.rest.dto.voucher.IssueVoucherRequest;
import com.aionn.promotion.adapter.rest.dto.voucher.response.VoucherResponse;
import com.aionn.promotion.adapter.rest.dto.voucher.response.MerchantVoucherAnalyticsResponse;
import com.aionn.promotion.adapter.rest.mapper.voucher.VoucherDtoMapper;
import com.aionn.promotion.adapter.rest.support.session.CurrentUserId;
import com.aionn.promotion.application.port.in.analytics.GetMerchantVoucherAnalyticsInputPort;
import com.aionn.promotion.application.port.in.voucher.IssueShopVoucherInputPort;
import com.aionn.promotion.application.port.in.voucher.ListMyShopVouchersInputPort;
import com.aionn.promotion.application.port.in.voucher.ListShopVouchersByMerchantInputPort;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/promotions/shop-vouchers")
@RequiredArgsConstructor
@Tag(name = "Promotion - Shop Voucher", description = "Merchant-issued vouchers scoped to one shop")
public class ShopVoucherController {

        private final IssueShopVoucherInputPort issueShopVoucherInputPort;
        private final ListMyShopVouchersInputPort listMyShopVouchersInputPort;
        private final ListShopVouchersByMerchantInputPort listShopVouchersByMerchantInputPort;
        private final GetMerchantVoucherAnalyticsInputPort getMerchantVoucherAnalyticsInputPort;
        private final VoucherDtoMapper dtoMapper;

        @GetMapping("/analytics")
        @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
        @Operation(summary = "Voucher redemption analytics for the authenticated merchant")
        public ResponseEntity<ApiResponse<MerchantVoucherAnalyticsResponse>> analytics(@CurrentUserId String ownerId) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponse(getMerchantVoucherAnalyticsInputPort.execute(ownerId)),
                                "Voucher analytics fetched"));
        }

        @PostMapping
        @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
        @Operation(summary = "Issue a voucher for the authenticated merchant's shop")
        public ResponseEntity<ApiResponse<VoucherResponse>> issue(
                        @CurrentUserId String ownerId,
                        @Valid @RequestBody IssueVoucherRequest request) {
                return ApiResponse.createdResponse("Shop voucher issued",
                                dtoMapper.toResponse(issueShopVoucherInputPort.execute(
                                                dtoMapper.toIssueShopVoucherCommand(ownerId, request))));
        }

        @GetMapping("/mine")
        @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
        @Operation(summary = "List vouchers issued by the authenticated merchant")
        public ResponseEntity<ApiResponse<List<VoucherResponse>>> listMine(
                        @CurrentUserId String ownerId,
                        @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponses(listMyShopVouchersInputPort.execute(ownerId, limit)),
                                "Shop vouchers fetched"));
        }

        @GetMapping("/merchant/{merchantId}")
        @Operation(summary = "List collectible vouchers for a public shop page")
        public ResponseEntity<ApiResponse<List<VoucherResponse>>> listByMerchant(
                        @PathVariable String merchantId,
                        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponses(listShopVouchersByMerchantInputPort.execute(merchantId, limit)),
                                "Shop vouchers fetched"));
        }
}
