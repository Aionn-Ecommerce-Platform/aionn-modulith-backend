package com.aionn.shipping.application.dto.rate.result;

import java.math.BigDecimal;
import java.time.Instant;

public record ShippingQuoteResult(
        BigDecimal fee,
        String currency,
        String zoneCode,
        String source,
        String detail,
        Instant estimatedDeliveryAt,
        Instant carrierOrderDate) {
}
