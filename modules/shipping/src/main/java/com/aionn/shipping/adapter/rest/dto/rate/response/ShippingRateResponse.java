package com.aionn.shipping.adapter.rest.dto.rate.response;

import java.math.BigDecimal;
import java.time.Instant;

public record ShippingRateResponse(
        String rateId,
        String zoneCode,
        BigDecimal baseFee,
        String currency,
        String condition,
        Instant createdAt,
        Instant updatedAt) {
}
