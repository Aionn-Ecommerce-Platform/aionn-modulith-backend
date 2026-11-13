package com.aionn.shipping.adapter.rest.dto.rate.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateRateRequest(
        @DecimalMin("0.0") BigDecimal baseFee,
        @Size(max = 1000) String condition,
        @Min(1) Integer baseWeightGram,
        @Min(1) Integer stepWeightGram,
        @DecimalMin("0.0") BigDecimal stepFee,
        @DecimalMin("0.0") BigDecimal maxFee,
        @DecimalMin("0.0") BigDecimal freeShippingThreshold,
        @DecimalMin("0.0") BigDecimal expressExtraFee,
        @Min(0) Integer expressEtaDays) {
}
