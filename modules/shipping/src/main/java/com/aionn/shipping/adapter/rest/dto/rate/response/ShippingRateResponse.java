package com.aionn.shipping.adapter.rest.dto.rate.response;

import java.math.BigDecimal;
import java.time.Instant;

public record ShippingRateResponse(
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
        BigDecimal expressExtraFee,
        Integer expressEtaDays,
        Instant createdAt,
        Instant updatedAt) {
}
