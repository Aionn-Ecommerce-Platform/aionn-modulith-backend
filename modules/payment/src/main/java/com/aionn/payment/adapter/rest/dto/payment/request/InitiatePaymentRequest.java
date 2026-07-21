package com.aionn.payment.adapter.rest.dto.payment.request;

import com.aionn.payment.domain.valueobject.PaymentGatewayKind;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record InitiatePaymentRequest(
        @NotBlank String orderId,
        String paymentMethodId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull PaymentGatewayKind gateway,
        @NotBlank @Size(max = 100) String idempotencyKey) {
}
