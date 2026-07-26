package com.aionn.promotion.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "promotion.defaults")
public record PromotionDefaultsProperties(
        @DefaultValue("VND") String currency) {
}
