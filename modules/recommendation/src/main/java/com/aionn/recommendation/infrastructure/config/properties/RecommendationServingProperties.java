package com.aionn.recommendation.infrastructure.config.properties;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Validated
@ConfigurationProperties(prefix = "recommendation.serving")
public record RecommendationServingProperties(
        @Min(1) @Max(3650) @DefaultValue("30") int seedLookbackDays,
        @Min(1) @Max(1000) @DefaultValue("20") int seedLimit,
        @Min(1) @Max(1000) @DefaultValue("50") int maxPageSize,
        @DecimalMin("0.0") @DecimalMax("1.0") @DefaultValue("0.40") BigDecimal maxFlashSaleShare,
        @Min(0) @Max(100) @DefaultValue("5") int categoryCandidateOvershoot) {
}
