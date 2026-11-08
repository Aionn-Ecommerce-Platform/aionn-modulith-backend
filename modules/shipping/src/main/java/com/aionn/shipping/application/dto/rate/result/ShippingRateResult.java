package com.aionn.shipping.application.dto.rate.result;

import java.math.BigDecimal;
import java.time.Instant;

public record ShippingRateResult(
        String rateId,
        String zoneCode,
        BigDecimal baseFee,
        String currency,
        String condition,
        int baseWeightGram,
        int stepWeightGram,
        BigDecimal stepFee,
        BigDecimal maxFee,
        BigDecimal freeShippingThreshold,
        Instant createdAt,
        Instant updatedAt) {
}
