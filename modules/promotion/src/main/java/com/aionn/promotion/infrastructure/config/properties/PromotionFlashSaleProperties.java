package com.aionn.promotion.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "promotion.flash-sale")
public record PromotionFlashSaleProperties(
        @DefaultValue("true") boolean enabled) {
}

