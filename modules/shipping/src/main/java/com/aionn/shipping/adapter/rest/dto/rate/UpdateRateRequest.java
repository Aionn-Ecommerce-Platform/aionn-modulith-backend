package com.aionn.shipping.adapter.rest.dto.rate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateRateRequest(
        @DecimalMin("0.0") BigDecimal baseFee,
        @Size(max = 1000) String condition) {
}

