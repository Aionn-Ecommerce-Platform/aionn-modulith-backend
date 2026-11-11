package com.aionn.recommendation.domain.valueobject;

import com.aionn.recommendation.domain.exception.RecommendationErrorCode;
import com.aionn.recommendation.domain.exception.RecommendationException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

/**
 * Base weight of an interaction plus the half-life over which it fades. Decay distinguishes this
 * module from catalog's browsing history: a purchase stays meaningful for months, a view for days.
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

    /** {@code base * 2^(-age / halfLife)}. Future-dated rows (clock skew) keep full weight, never more. */
    public BigDecimal decayedAt(Instant occurredAt, Instant now) {
        if (occurredAt == null || now == null) {
            throw new RecommendationException(RecommendationErrorCode.INTERACTION_INVALID,
                    "occurredAt and now must not be null");
        }
        Duration age = Duration.between(occurredAt, now);
        if (age.isNegative() || age.isZero()) {
            return baseWeight;
        }
        double ageSec = age.getSeconds() + (age.getNano() / 1_000_000_000.0);
        double halfLifeSec = halfLife.getSeconds() + (halfLife.getNano() / 1_000_000_000.0);
        double exponent = -ageSec / halfLifeSec;
        BigDecimal factor = BigDecimal.valueOf(Math.pow(2, exponent));
        return baseWeight.multiply(factor, DECAY_PRECISION);
    }
}
