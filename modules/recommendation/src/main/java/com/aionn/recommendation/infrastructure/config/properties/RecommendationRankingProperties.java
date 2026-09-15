package com.aionn.recommendation.infrastructure.config.properties;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

/**
 * Signal weights for hybrid ranking.
 *
 * <p>The three surface weights are relative - {@code HybridRankingPolicy} divides by their sum - so
 * they need not add to one, but they must not be negative: a negative weight would let a signal
 * subtract from a score and could drive the sum to zero or below, which makes every slate empty
 * without raising an error anywhere.
 */
@Validated
@ConfigurationProperties(prefix = "recommendation.ranking")
public record RecommendationRankingProperties(
        @DecimalMin("0.0") @DecimalMax("10.0") @DefaultValue("0.50") BigDecimal collaborativeWeight,
        @DecimalMin("0.0") @DecimalMax("10.0") @DefaultValue("0.35") BigDecimal contentWeight,
        @DecimalMin("0.0") @DecimalMax("10.0") @DefaultValue("0.15") BigDecimal popularityWeight,
        @DecimalMin("0.0") @DecimalMax("10.0") @DefaultValue("0.45") BigDecimal categoryAffinityWeight,
        @DecimalMin("0.0") @DecimalMax("10.0") @DefaultValue("0.35") BigDecimal brandAffinityWeight,
        @DecimalMin("0.0") @DecimalMax("10.0") @DefaultValue("0.20") BigDecimal priceFitWeight,
        @Min(1) @Max(10) @DefaultValue("3") int candidateOverFetchFactor,
        @Min(1) @Max(1000) @DefaultValue("200") int maxCandidates) {
}
