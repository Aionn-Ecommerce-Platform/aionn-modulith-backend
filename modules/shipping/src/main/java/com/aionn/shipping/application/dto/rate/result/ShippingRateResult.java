package com.aionn.shipping.application.dto.rate.result;

import java.math.BigDecimal;
import java.time.Instant;

public record ShippingRateResult(
        String rateId,
        String zoneCode,
        BigDecimal baseFee,
        String currency,
        String condition,
        Instant createdAt,
        Instant updatedAt) {
}

