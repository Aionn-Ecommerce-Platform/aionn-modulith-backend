package com.aionn.shipping.application.dto.rate.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ShippingQuoteResult(
        BigDecimal fee,
        String currency,
        String zoneCode,
        String source,
        String detail,
        Instant estimatedDeliveryAt,
        Instant carrierOrderDate,
        List<ShippingOption> options) {

    public ShippingQuoteResult(
            BigDecimal fee,
            String currency,
            String zoneCode,
            String source,
            String detail,
            Instant estimatedDeliveryAt,
            Instant carrierOrderDate) {
        this(fee, currency, zoneCode, source, detail, estimatedDeliveryAt, carrierOrderDate, List.of());
    }

    public record ShippingOption(
            String code,
            String label,
            BigDecimal fee,
            Instant estimatedDeliveryAt) {
    }
}
