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
     * Bounds for the offline rebuilds.
     *
     * <p>{@code computeTimeoutSeconds} overrides the application-wide
     * {@code spring.transaction.default-timeout} for the heavy read and each batched write. The co-occurrence
     * self-join and the decayed-popularity aggregate both scan the interaction log, so at volume they
     * legitimately need longer than a request-scoped transaction; leaving them on the 30s default
     * makes the job fail as soon as the data is worth computing.
     *
     * <p>{@code upsertBatchSize} bounds each write transaction, so the rebuild commits progressively
     * instead of holding one transaction open across the whole result set.
     *
     * <p>The timeout is per transaction, not a whole-rebuild deadline. The scheduler's JDBC lease
     * is renewed independently throughout the run, including between batches. The 29-minute ceiling
     * keeps an individual transaction below the initial thirty-minute lease with a safety margin.
     */
    public record Execution(
            @Min(1) @Max(1740) @DefaultValue("900") int computeTimeoutSeconds,
            @Min(1) @Max(10_000) @DefaultValue("500") int upsertBatchSize) {
    }
}
