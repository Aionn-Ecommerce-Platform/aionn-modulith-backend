package com.aionn.recommendation.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Windows and batch sizes for the offline jobs. All expressed in days or rows rather than durations,
 * matching how the values are reasoned about operationally.
 */
@ConfigurationProperties(prefix = "recommendation")
public record RecommendationJobProperties(
        @DefaultValue Profile profile,
        @DefaultValue Similarity similarity,
        @DefaultValue Popularity popularity,
        @DefaultValue Retention retention) {

    public record Profile(
            @DefaultValue("10") int maxAffinities,
            @DefaultValue("180") long lookbackDays) {
    }

    public record Similarity(
            @DefaultValue("180") long lookbackDays,
            @DefaultValue("2") int minCoOccurrence,
            @DefaultValue("50") int maxNeighboursPerProduct) {
    }

    public record Popularity(@DefaultValue("30") long lookbackDays) {
    }

    public record Retention(@DefaultValue("180") long interactionMaxAgeDays) {
    }
}
