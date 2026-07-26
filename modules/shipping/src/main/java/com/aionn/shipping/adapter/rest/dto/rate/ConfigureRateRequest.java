package com.aionn.shipping.adapter.rest.dto.rate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ConfigureRateRequest(
        @NotBlank @Size(max = 50) String zoneCode,
        @NotNull @DecimalMin("0.0") BigDecimal baseFee,
        @Size(min = 3, max = 3) String currency,
        @Size(max = 1000) String condition) {
}

