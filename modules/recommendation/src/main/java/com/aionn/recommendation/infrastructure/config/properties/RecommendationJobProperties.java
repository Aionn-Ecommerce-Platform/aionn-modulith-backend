package com.aionn.recommendation.infrastructure.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Windows and batch sizes for the offline jobs. All expressed in days or rows rather than durations,
 * matching how the values are reasoned about operationally.
 */
@Validated
@ConfigurationProperties(prefix = "recommendation")
public record RecommendationJobProperties(
        @NotNull @Valid @DefaultValue Profile profile,
        @NotNull @Valid @DefaultValue Similarity similarity,
        @NotNull @Valid @DefaultValue Popularity popularity,
        @NotNull @Valid @DefaultValue Retention retention,
        @NotNull @Valid @DefaultValue Execution execution) {

    public record Profile(
            @Min(1) @Max(100) @DefaultValue("10") int maxAffinities,
            @Min(1) @Max(3650) @DefaultValue("180") long lookbackDays) {
    }

    public record Similarity(
            @Min(1) @Max(3650) @DefaultValue("180") long lookbackDays,
            @Min(1) @Max(1000) @DefaultValue("2") int minCoOccurrence,
            @Min(1) @Max(1000) @DefaultValue("50") int maxNeighboursPerProduct) {
    }

    public record Popularity(@Min(1) @Max(3650) @DefaultValue("30") long lookbackDays) {
    }

    public record Retention(@Min(1) @Max(3650) @DefaultValue("180") long interactionMaxAgeDays) {
    }

    /**
     * Bounds for the offline rebuilds. {@code computeTimeoutSeconds} overrides the request-sized
     * transaction default because both rebuilds scan the interaction log; it stays under the scheduler's
     * 30-minute lease. {@code upsertBatchSize} bounds each write transaction so commits are progressive.
     */
    public record Execution(
            @Min(1) @Max(1740) @DefaultValue("900") int computeTimeoutSeconds,
            @Min(1) @Max(10_000) @DefaultValue("500") int upsertBatchSize) {
    }
}
