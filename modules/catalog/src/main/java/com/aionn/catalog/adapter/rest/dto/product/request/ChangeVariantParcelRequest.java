package com.aionn.catalog.adapter.rest.dto.product.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Parcel data for one SKU. {@code weightGram} is required; the box is optional
 * but must be supplied as a complete L×W×H triple when present.
 */
public record ChangeVariantParcelRequest(
        @NotNull @Positive Integer weightGram,
        BigDecimal lengthCm,
        BigDecimal widthCm,
        BigDecimal heightCm) {
}
