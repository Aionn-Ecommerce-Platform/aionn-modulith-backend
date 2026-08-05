package com.aionn.promotion.adapter.rest.controller;

import com.aionn.promotion.adapter.rest.dto.voucher.ApplyVoucherRequest;
import com.aionn.promotion.adapter.rest.dto.voucher.ReleaseVoucherRequest;
import com.aionn.promotion.adapter.rest.dto.voucher.ReserveVoucherRequest;
import com.aionn.promotion.adapter.rest.dto.voucher.response.UserVoucherResponse;
import com.aionn.promotion.adapter.rest.mapper.voucher.VoucherDtoMapper;
import com.aionn.promotion.adapter.rest.support.session.CurrentUserId;
import com.aionn.promotion.application.port.in.voucher.ApplyVoucherInputPort;
import com.aionn.promotion.application.port.in.voucher.ClaimVoucherInputPort;
import com.aionn.promotion.application.port.in.voucher.GetMyVoucherInputPort;
import com.aionn.promotion.application.port.in.voucher.ListMyVouchersInputPort;
import com.aionn.promotion.application.port.in.voucher.ReleaseVoucherInputPort;
import com.aionn.promotion.application.port.in.voucher.ReserveVoucherInputPort;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import com.aionn.sharedkernel.adapter.web.support.idempotency.IdempotentRequest;
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
@RequestMapping("/api/v1/promotions/vouchers")
@RequiredArgsConstructor
@Tag(name = "Promotion - Voucher", description = "User voucher claim/reserve/apply/release")
public class VoucherController {

        private final ClaimVoucherInputPort claimVoucherInputPort;
        private final ReserveVoucherInputPort reserveVoucherInputPort;
        private final ApplyVoucherInputPort applyVoucherInputPort;
        private final ReleaseVoucherInputPort releaseVoucherInputPort;
        private final ListMyVouchersInputPort listMyVouchersInputPort;
        private final GetMyVoucherInputPort getMyVoucherInputPort;
        private final VoucherDtoMapper dtoMapper;

        @PostMapping("/{voucherCode}/claim")
        @IdempotentRequest(ttlSeconds = 300)
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Claim voucher")
        public ResponseEntity<ApiResponse<UserVoucherResponse>> claim(
                        @CurrentUserId String userId,
                        @PathVariable String voucherCode) {
                return ApiResponse.createdResponse("Voucher claimed",
                                dtoMapper.toResponse(claimVoucherInputPort.execute(
                                                dtoMapper.toClaimCommand(userId, voucherCode))));
        }

        @PostMapping("/{voucherCode}/reserve")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Reserve voucher for an order")
        public ResponseEntity<ApiResponse<UserVoucherResponse>> reserve(
                        @CurrentUserId String userId,
                        @PathVariable String voucherCode,
                        @Valid @RequestBody ReserveVoucherRequest request) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponse(reserveVoucherInputPort.execute(
                                                dtoMapper.toReserveCommand(userId, voucherCode, request))),
                                "Voucher reserved"));
        }

        @PostMapping("/{voucherCode}/apply")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Apply voucher to a paid order")
        public ResponseEntity<ApiResponse<UserVoucherResponse>> apply(
                        @CurrentUserId String userId,
                        @PathVariable String voucherCode,
                        @Valid @RequestBody ApplyVoucherRequest request) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponse(applyVoucherInputPort.execute(
                                                dtoMapper.toApplyCommand(userId, voucherCode, request))),
                                "Voucher applied"));
        }

        @PostMapping("/{voucherCode}/release")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Release voucher reservation")
        public ResponseEntity<ApiResponse<UserVoucherResponse>> release(
                        @CurrentUserId String userId,
                        @PathVariable String voucherCode,
                        @Valid @RequestBody ReleaseVoucherRequest request) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponse(releaseVoucherInputPort.execute(
                                                dtoMapper.toReleaseCommand(userId, voucherCode, request))),
                                "Voucher released"));
        }

        @GetMapping("/me")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "List my vouchers")
        public ResponseEntity<ApiResponse<List<UserVoucherResponse>>> listMine(
                        @CurrentUserId String userId,
                        @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toUserVoucherResponses(
                                                listMyVouchersInputPort.execute(userId, limit)),
                                "Vouchers fetched"));
        }

        @GetMapping("/me/{voucherCode}")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Get my voucher")
        public ResponseEntity<ApiResponse<UserVoucherResponse>> getMine(
                        @CurrentUserId String userId,
                        @PathVariable String voucherCode) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponse(getMyVoucherInputPort.execute(userId, voucherCode)),
                                "Voucher fetched"));
        }
}
