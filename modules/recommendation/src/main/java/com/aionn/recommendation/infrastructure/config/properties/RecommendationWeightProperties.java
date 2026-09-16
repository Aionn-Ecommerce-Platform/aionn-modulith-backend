package com.aionn.recommendation.infrastructure.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

/**
 * Interaction weights and half-lives. Half-lives are expressed in days because that is the unit the
 * business reasons in; the domain converts to a {@link java.time.Duration}.
 *
 * <p>The bounds are the ones that keep a bad value from failing at runtime instead of at startup. A
 * zero half-life makes {@code InteractionWeight} throw on every ingest, and a base weight above the
 * {@code NUMERIC(5,2)} scale of {@code recommendation_interactions.weight} overflows the column -
 * either way the application starts healthy and then silently loses every signal.
 */
@Validated
@ConfigurationProperties(prefix = "recommendation.weights")
public record RecommendationWeightProperties(
        @NotNull @Valid @DefaultValue Signal view,
        @NotNull @Valid @DefaultValue Signal cartAdd,
        @NotNull @Valid @DefaultValue Signal purchase) {

    /**
     * @param base         floor of 0.01 because the column stores two decimal places and the table
     *                     constrains weight to be strictly positive; a smaller value would round to
     *                     zero and be rejected on insert
     * @param halfLifeDays floor of 1 because a zero duration makes the domain value object throw
     */
    public record Signal(
            @DecimalMin("0.01") @DecimalMax("999.99") @DefaultValue("1.0") BigDecimal base,
            @Min(1) @Max(3650) @DefaultValue("30") long halfLifeDays) {
    }
}
