package com.aionn.catalog.adapter.rest.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Map;

public record DefineVariantRequest(
        @NotBlank String skuId,
        Map<String, String> attributeValues,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
        @NotBlank String currency) {
}
