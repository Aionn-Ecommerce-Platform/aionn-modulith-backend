package com.aionn.recommendation.infrastructure.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Cold-start thresholds, counted in interactions.
 *
 * <p>{@code fullHybridThreshold} below {@code contentOnlyThreshold} is not rejected here: the policy
 * resolves it with {@code Math.max} so an inverted pair degrades to content-only rather than failing
 * at startup. The bounds only keep the values inside a range where counting interactions is
 * meaningful.
 */
@Validated
@ConfigurationProperties(prefix = "recommendation.cold-start")
public record RecommendationColdStartProperties(
        @Min(1) @Max(1000) @DefaultValue("1") int contentOnlyThreshold,
        @Min(1) @Max(1000) @DefaultValue("5") int fullHybridThreshold) {
}
