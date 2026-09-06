package com.aionn.recommendation.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.math.BigDecimal;

/**
 * Interaction weights and half-lives. Half-lives are expressed in days because that is the unit the
 * business reasons in; the domain converts to a {@link java.time.Duration}.
 */
@ConfigurationProperties(prefix = "recommendation.weights")
public record RecommendationWeightProperties(
        @DefaultValue Signal view,
        @DefaultValue Signal cartAdd,
        @DefaultValue Signal purchase) {

    public record Signal(
            @DefaultValue("1.0") BigDecimal base,
            @DefaultValue("30") long halfLifeDays) {
    }
}
