package com.aionn.promotion.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "promotion.voucher")
public record PromotionVoucherProperties(
        @DefaultValue("900") int reservationTtlSeconds,
        @DefaultValue AutoRelease autoRelease) {

    public record AutoRelease(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("30000") long delayMs,
            @DefaultValue("100") int batchSize) {
    }
}
