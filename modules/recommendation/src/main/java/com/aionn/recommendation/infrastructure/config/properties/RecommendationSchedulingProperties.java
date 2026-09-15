package com.aionn.recommendation.infrastructure.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Cadence and batch size of the four offline jobs.
 *
 * <p>{@code delayMs} has a floor of one second because a shorter fixed delay turns the job into a
 * busy loop against the database, and a ceiling of seven days because a longer interval means the
 * derived tables are stale by more than any useful definition.
 */
@Validated
@ConfigurationProperties(prefix = "recommendation.scheduling")
public record RecommendationSchedulingProperties(
        @NotNull @Valid @DefaultValue Job profileRefresh,
        @NotNull @Valid @DefaultValue Job itemSimilarity,
        @NotNull @Valid @DefaultValue Job popularity,
        @NotNull @Valid @DefaultValue Job prune) {

    public record Job(
            @DefaultValue("true") boolean enabled,
            @Min(1000) @Max(604_800_000L) @DefaultValue("900000") long delayMs,
            @Min(1) @Max(10_000) @DefaultValue("500") int batchSize) {
    }
}
