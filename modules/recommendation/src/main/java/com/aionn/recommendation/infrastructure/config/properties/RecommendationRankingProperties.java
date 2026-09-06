package com.aionn.recommendation.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "recommendation.ranking")
public record RecommendationRankingProperties(
        @DefaultValue("0.50") BigDecimal collaborativeWeight,
        @DefaultValue("0.35") BigDecimal contentWeight,
        @DefaultValue("0.15") BigDecimal popularityWeight,
        @DefaultValue("0.45") BigDecimal categoryAffinityWeight,
        @DefaultValue("0.35") BigDecimal brandAffinityWeight,
        @DefaultValue("0.20") BigDecimal priceFitWeight,
        @DefaultValue("3") int candidateOverFetchFactor,
        @DefaultValue("200") int maxCandidates) {
}
