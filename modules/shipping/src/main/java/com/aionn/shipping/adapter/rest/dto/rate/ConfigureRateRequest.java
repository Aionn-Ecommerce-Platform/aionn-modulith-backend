package com.aionn.shipping.adapter.rest.dto.rate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Zone shipping rate in the Vietnam marketplace shape: a flat fee covering the
 * first {@code baseWeightGram}, plus {@code stepFee} per (or part)
 * {@code stepWeightGram} beyond it, optionally capped and waived above a goods
 * subtotal. Omitted weight-ladder fields fall back to 1 kg / 500 g / 0đ.
 */
public record ConfigureRateRequest(
        @NotBlank @Size(max = 50) String zoneCode,
        @NotNull @DecimalMin("0.0") BigDecimal baseFee,
        @Size(min = 3, max = 3) String currency,
        @Size(max = 1000) String condition,
        @Min(1) Integer baseWeightGram,
        @Min(1) Integer stepWeightGram,
        @DecimalMin("0.0") BigDecimal stepFee,
        @DecimalMin("0.0") BigDecimal maxFee,
        @DecimalMin("0.0") BigDecimal freeShippingThreshold) {
}
