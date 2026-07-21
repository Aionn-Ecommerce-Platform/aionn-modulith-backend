package com.aionn.payment.adapter.rest.controller;

import com.aionn.payment.adapter.rest.dto.payment.request.InitiatePaymentRequest;
import com.aionn.payment.adapter.rest.dto.payment.request.RefundRequest;
import com.aionn.payment.adapter.rest.dto.payment.response.PaymentResponse;
import com.aionn.payment.adapter.rest.mapper.payment.PaymentDtoMapper;
import com.aionn.payment.adapter.rest.support.session.CurrentUserId;
import com.aionn.payment.application.dto.payment.command.InitiatePaymentCommand;
import com.aionn.payment.application.dto.payment.command.RefundPaymentCommand;
import com.aionn.payment.application.port.in.payment.GetPaymentForUserInputPort;
import com.aionn.payment.application.port.in.payment.InitiatePaymentInputPort;
import com.aionn.payment.application.port.in.payment.ListPaymentsByOrderInputPort;
import com.aionn.payment.application.port.in.payment.RefundPaymentInputPort;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment - Payment", description = "Payment lifecycle endpoints")
public class PaymentController {

    private final InitiatePaymentInputPort initiatePaymentInputPort;
    private final RefundPaymentInputPort refundPaymentInputPort;
    private final GetPaymentForUserInputPort getPaymentForUserInputPort;
    private final ListPaymentsByOrderInputPort listPaymentsByOrderInputPort;
    private final PaymentDtoMapper paymentDtoMapper;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Initiate payment")
    public ResponseEntity<ApiResponse<PaymentResponse>> initiate(
            @CurrentUserId String userId,
            @Valid @RequestBody InitiatePaymentRequest request) {
        InitiatePaymentCommand command = paymentDtoMapper.toCommand(request, userId, request.idempotencyKey());
        return ApiResponse.createdResponse("Payment initiated",
                paymentDtoMapper.toResponse(initiatePaymentInputPort.execute(command)));
    }

    @PostMapping("/{paymentId}/refund")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
    @Operation(summary = "Refund payment")
    public ResponseEntity<ApiResponse<PaymentResponse>> refund(
            @PathVariable String paymentId,
            @Valid @RequestBody RefundRequest request) {
        RefundPaymentCommand command = paymentDtoMapper.toCommand(paymentId, request);
        return ResponseEntity.ok(ApiResponse.success(
                paymentDtoMapper.toResponse(refundPaymentInputPort.execute(command)), "Payment refunded"));
    }

    @GetMapping("/{paymentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get payment")
    public ResponseEntity<ApiResponse<PaymentResponse>> get(
            @CurrentUserId String userId,
            @PathVariable String paymentId) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentDtoMapper.toResponse(getPaymentForUserInputPort.execute(paymentId, userId)),
                "Payment fetched"));
    }

    @GetMapping("/by-order/{orderId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
    @Operation(summary = "List payments by order")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> listByOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentDtoMapper.toResponses(listPaymentsByOrderInputPort.execute(orderId)),
                "Payments fetched"));
    }
}
