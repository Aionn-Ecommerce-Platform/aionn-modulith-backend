package com.aionn.promotion.infrastructure.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "promotion.limits")
public record PromotionLimitProperties(
        @Min(1) @Max(1000) @DefaultValue("100") int maxListLimit,
        @Min(1) @Max(1000) @DefaultValue("200") int maxRegistrationLimit,
        @Min(1) @Max(1000) @DefaultValue("50") int maxActiveFlashSaleLimit,
        @Min(1) @Max(100) @DefaultValue("10") int topVouchersLimit) {
}
