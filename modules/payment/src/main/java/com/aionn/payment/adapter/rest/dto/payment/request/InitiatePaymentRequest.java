package com.aionn.payment.adapter.rest.dto.payment.request;

import com.aionn.payment.domain.valueobject.PaymentGatewayKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InitiatePaymentRequest(
        @NotBlank String orderId,
        String paymentMethodId,
        @NotNull PaymentGatewayKind gateway,
        @NotBlank @Size(max = 100) String idempotencyKey) {
}
