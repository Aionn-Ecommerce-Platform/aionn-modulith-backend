package com.aionn.payment.adapter.rest.dto.payout.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PayoutRequestBody(
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        String currency,
        @NotBlank String bankName,
        @NotBlank String bankAccountNo,
        @NotBlank String bankAccountName,
        String note) {
}
