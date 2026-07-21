package com.aionn.payment.adapter.rest.controller;

import com.aionn.payment.adapter.rest.dto.payout.request.PayoutCompleteBody;
import com.aionn.payment.adapter.rest.dto.payout.request.PayoutFailBody;
import com.aionn.payment.adapter.rest.dto.payout.response.PayoutResponse;
import com.aionn.payment.adapter.rest.mapper.payout.PayoutDtoMapper;
import com.aionn.payment.application.port.in.payout.CompletePayoutInputPort;
import com.aionn.payment.application.port.in.payout.FailPayoutInputPort;
import com.aionn.payment.application.port.in.payout.ListPayoutsByStatusInputPort;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/payouts")
@RequiredArgsConstructor
@Tag(name = "Payment - Admin Payouts", description = "Admin payout management")
public class AdminPayoutController {

    private final ListPayoutsByStatusInputPort listPayoutsByStatusInputPort;
    private final CompletePayoutInputPort completePayoutInputPort;
    private final FailPayoutInputPort failPayoutInputPort;
    private final PayoutDtoMapper payoutDtoMapper;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
    @Operation(summary = "List payouts by status")
    public ResponseEntity<ApiResponse<List<PayoutResponse>>> list(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(ApiResponse.success(
                payoutDtoMapper.toResponses(listPayoutsByStatusInputPort.execute(status, limit)),
                "Payouts fetched"));
    }

    @PostMapping("/{payoutId}/complete")
    @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Mark payout completed (after bank transfer)")
    public ResponseEntity<ApiResponse<PayoutResponse>> complete(
            @PathVariable String payoutId,
            @Valid @RequestBody PayoutCompleteBody body) {
        return ResponseEntity.ok(ApiResponse.success(
                payoutDtoMapper.toResponse(completePayoutInputPort.execute(payoutId, body.externalRef())),
                "Payout completed"));
    }

    @PostMapping("/{payoutId}/fail")
    @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Mark payout failed and refund balance")
    public ResponseEntity<ApiResponse<PayoutResponse>> fail(
            @PathVariable String payoutId,
            @Valid @RequestBody PayoutFailBody body) {
        return ResponseEntity.ok(ApiResponse.success(
                payoutDtoMapper.toResponse(failPayoutInputPort.execute(payoutId, body.reason())),
                "Payout marked failed"));
    }
}
