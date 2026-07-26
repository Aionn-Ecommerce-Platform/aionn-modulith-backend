package com.aionn.promotion.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "promotion.scheduler")
public record PromotionSchedulerProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("30000") long delayMs,
        @DefaultValue("100") int batchSize) {
}
