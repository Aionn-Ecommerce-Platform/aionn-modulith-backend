package com.aionn.catalog.adapter.rest.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ChangeVariantPriceRequest(
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal newPrice,
        @NotBlank String currency) {
}
