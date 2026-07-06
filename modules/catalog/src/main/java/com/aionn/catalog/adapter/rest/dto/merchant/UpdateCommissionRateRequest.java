package com.aionn.catalog.adapter.rest.dto.merchant;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateCommissionRateRequest(
        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("1.0")
        BigDecimal commissionRate) {
}
