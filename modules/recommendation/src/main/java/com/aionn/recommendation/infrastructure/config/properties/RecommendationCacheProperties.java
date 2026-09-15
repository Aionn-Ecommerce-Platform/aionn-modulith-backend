package com.aionn.recommendation.infrastructure.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Cache tuning. L2 TTLs are set to match the refresh cadence of the job that produces each dataset:
 * caching longer only serves staler data, caching shorter recomputes an identical answer.
 */
@Validated
@ConfigurationProperties(prefix = "recommendation.cache")
public record RecommendationCacheProperties(
        @NotNull @Valid @DefaultValue Tier home,
        @NotNull @Valid @DefaultValue Tier similar,
        @NotNull @Valid @DefaultValue Tier trending) {

    public record Tier(
            @Min(1) @Max(86_400) @DefaultValue("60") long l1TtlSeconds,
            @Min(1) @Max(1_000_000) @DefaultValue("1000") long l1MaxSize,
            @Min(1) @Max(604_800) @DefaultValue("900") long l2TtlSeconds) {
    }
}
