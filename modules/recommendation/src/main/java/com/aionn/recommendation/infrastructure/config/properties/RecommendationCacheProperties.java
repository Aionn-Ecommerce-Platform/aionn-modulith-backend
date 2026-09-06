package com.aionn.recommendation.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Cache tuning. L2 TTLs are set to match the refresh cadence of the job that produces each dataset:
 * caching longer only serves staler data, caching shorter recomputes an identical answer.
 */
@ConfigurationProperties(prefix = "recommendation.cache")
public record RecommendationCacheProperties(
        @DefaultValue Tier home,
        @DefaultValue Tier similar,
        @DefaultValue Tier trending) {

    public record Tier(
            @DefaultValue("60") long l1TtlSeconds,
            @DefaultValue("1000") long l1MaxSize,
            @DefaultValue("900") long l2TtlSeconds) {
    }
}
