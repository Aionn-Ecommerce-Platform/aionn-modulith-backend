package com.aionn.recommendation.domain.valueobject;

import com.aionn.recommendation.domain.exception.RecommendationErrorCode;
import com.aionn.recommendation.domain.exception.RecommendationException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

/**
 * Base weight of an interaction plus the half-life over which it fades.
 *
 * <p>Decay is what separates this module from catalog's browsing history, which treats a view from
 * six months ago exactly like one from this morning. A purchase stays meaningful for months; a view
 * stops meaning much within a fortnight.
 */
public record InteractionWeight(BigDecimal baseWeight, Duration halfLife) {

    private static final MathContext DECAY_PRECISION = new MathContext(8, RoundingMode.HALF_UP);

    public InteractionWeight {
        if (baseWeight == null || baseWeight.signum() <= 0) {
            throw new RecommendationException(RecommendationErrorCode.INTERACTION_INVALID,
                    "baseWeight must be positive");
        }
        if (halfLife == null || halfLife.isNegative() || halfLife.isZero()) {
            throw new RecommendationException(RecommendationErrorCode.INTERACTION_INVALID,
                    "halfLife must be positive");
        }
    }

    public static InteractionWeight of(BigDecimal baseWeight, Duration halfLife) {
        return new InteractionWeight(baseWeight, halfLife);
    }

    /**
     * Exponential decay: {@code base * 2^(-age / halfLife)}. The caller supplies {@code now} because
     * domain code must not read the system clock.
     *
     * <p>Interactions dated in the future (clock skew between instances) keep their full weight
     * rather than being amplified.
     */
    public BigDecimal decayedAt(Instant occurredAt, Instant now) {
        if (occurredAt == null || now == null) {
            throw new RecommendationException(RecommendationErrorCode.INTERACTION_INVALID,
                    "occurredAt and now must not be null");
        }
        long ageSeconds = Duration.between(occurredAt, now).getSeconds();
        if (ageSeconds <= 0) {
            return baseWeight;
        }
        double exponent = -((double) ageSeconds) / halfLife.getSeconds();
        BigDecimal factor = BigDecimal.valueOf(Math.pow(2, exponent));
        return baseWeight.multiply(factor, DECAY_PRECISION);
    }
}
