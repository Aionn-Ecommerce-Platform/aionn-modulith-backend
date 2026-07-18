package com.aionn.ordering.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "ordering.shipping")
public record OrderingShippingProperties(
        @DefaultValue("assume-success") String provider,
        @DefaultValue("30000") BigDecimal defaultQuoteAmount,
        @DefaultValue("VND") String defaultQuoteCurrency) {
}
