package com.aionn.catalog.adapter.rest.dto.product.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.Map;

public record DefineVariantRequest(
        @NotBlank String skuId,
        Map<String, String> attributeValues,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
        @NotBlank String currency,
        /**
         * Shipping weight per sellable unit (grams). Required before publish —
         * the carrier fee is quoted from this value, never a guessed parcel.
         */
        @NotNull @Positive Integer weightGram,
        /** Optional parcel box (cm). All three must be provided together. */
        BigDecimal lengthCm,
        BigDecimal widthCm,
        BigDecimal heightCm) {
}
