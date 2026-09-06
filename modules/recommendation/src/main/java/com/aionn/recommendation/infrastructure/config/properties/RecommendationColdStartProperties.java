package com.aionn.recommendation.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "recommendation.cold-start")
public record RecommendationColdStartProperties(
        @DefaultValue("1") int contentOnlyThreshold,
        @DefaultValue("5") int fullHybridThreshold) {
}
