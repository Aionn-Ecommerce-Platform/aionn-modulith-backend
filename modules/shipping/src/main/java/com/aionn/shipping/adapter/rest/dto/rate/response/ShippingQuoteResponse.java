package com.aionn.shipping.adapter.rest.dto.rate.response;

import java.math.BigDecimal;
import java.time.Instant;

public record ShippingQuoteResponse(
        BigDecimal fee,
        String currency,
        String zoneCode,
        String source,
        String detail,
        Instant estimatedDeliveryAt,
        Instant carrierOrderDate) {
}
