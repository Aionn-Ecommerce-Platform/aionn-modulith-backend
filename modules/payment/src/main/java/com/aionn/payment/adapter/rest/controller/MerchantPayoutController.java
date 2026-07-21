package com.aionn.payment.adapter.rest.controller;

import com.aionn.payment.adapter.rest.dto.payout.request.PayoutRequestBody;
import com.aionn.payment.adapter.rest.dto.payout.response.MerchantBalanceResponse;
import com.aionn.payment.adapter.rest.dto.payout.response.PayoutResponse;
import com.aionn.payment.adapter.rest.mapper.payout.PayoutDtoMapper;
import com.aionn.payment.adapter.rest.support.session.CurrentUserId;
import com.aionn.payment.application.dto.payout.command.RequestPayoutCommand;
import com.aionn.payment.application.port.in.payout.GetMerchantBalanceInputPort;
import com.aionn.payment.application.port.in.payout.ListMerchantPayoutsInputPort;
import com.aionn.payment.application.port.in.payout.RequestPayoutInputPort;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments/merchant")
@RequiredArgsConstructor
@Tag(name = "Payment - Merchant Payouts", description = "Merchant balance + payout request endpoints")
public class MerchantPayoutController {

    private final GetMerchantBalanceInputPort getMerchantBalanceInputPort;
    private final ListMerchantPayoutsInputPort listMerchantPayoutsInputPort;
    private final RequestPayoutInputPort requestPayoutInputPort;
    private final PayoutDtoMapper payoutDtoMapper;

    @GetMapping("/balance")
    @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
    @Operation(summary = "Get my balance")
    public ResponseEntity<ApiResponse<MerchantBalanceResponse>> getBalance(
            @CurrentUserId String ownerId,
            @RequestParam(defaultValue = "VND") String currency) {
        return ResponseEntity.ok(ApiResponse.success(
                payoutDtoMapper.toResponse(getMerchantBalanceInputPort.execute(ownerId, currency)),
                "Balance fetched"));
    }

    @GetMapping("/payouts")
    @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
    @Operation(summary = "List my payouts")
    public ResponseEntity<ApiResponse<List<PayoutResponse>>> list(
            @CurrentUserId String ownerId,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(ApiResponse.success(
                payoutDtoMapper.toResponses(listMerchantPayoutsInputPort.execute(ownerId, limit)),
                "Payouts fetched"));
    }

    @PostMapping("/payouts")
    @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
    @Operation(summary = "Request a payout")
    public ResponseEntity<ApiResponse<PayoutResponse>> request(
            @CurrentUserId String ownerId,
            @Valid @RequestBody PayoutRequestBody body) {
        RequestPayoutCommand command = payoutDtoMapper.toCommand(ownerId, body);
        return ApiResponse.createdResponse("Payout requested",
                payoutDtoMapper.toResponse(requestPayoutInputPort.execute(command)));
    }
}
