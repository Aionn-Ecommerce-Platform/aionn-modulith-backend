package com.aionn.shipping.adapter.rest.dto.rate.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ShippingQuoteResponse(
        BigDecimal fee,
        String currency,
        String zoneCode,
        String source,
        String detail,
        Instant estimatedDeliveryAt,
        Instant carrierOrderDate,
        List<ShippingOptionResponse> options) {

    public record ShippingOptionResponse(
            String code,
            String label,
            BigDecimal fee,
            Instant estimatedDeliveryAt) {
    }
}
