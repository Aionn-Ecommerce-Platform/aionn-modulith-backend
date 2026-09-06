package com.aionn.recommendation.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "recommendation.scheduling")
public record RecommendationSchedulingProperties(
        @DefaultValue Job profileRefresh,
        @DefaultValue Job itemSimilarity,
        @DefaultValue Job popularity,
        @DefaultValue Job prune) {

    public record Job(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("900000") long delayMs,
            @DefaultValue("500") int batchSize) {
    }
}
