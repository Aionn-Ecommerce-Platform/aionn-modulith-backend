package com.aionn.recommendation.domain.valueobject;

import com.aionn.recommendation.domain.exception.RecommendationErrorCode;
import com.aionn.recommendation.domain.exception.RecommendationException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A normalised score in [0,1].
 *
 * <p>Normalisation is a correctness requirement, not tidiness: hybrid ranking adds a cosine
 * similarity (always <= 1) to a decayed popularity total (potentially in the thousands). Without a
 * shared scale, popularity would swallow the configured weights and the other signals would have no
 * effect.
 */
public record AffinityScore(BigDecimal value) implements Comparable<AffinityScore> {

    private static final int SCALE = 5;

    public static final AffinityScore ZERO = new AffinityScore(BigDecimal.ZERO);
    public static final AffinityScore ONE = new AffinityScore(BigDecimal.ONE);

    public AffinityScore {
        if (value == null) {
            throw new RecommendationException(RecommendationErrorCode.INVALID_ARGUMENT,
                    "score value must not be null");
        }
        if (value.signum() < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new RecommendationException(RecommendationErrorCode.INVALID_ARGUMENT,
                    "score must be within [0,1], got " + value.toPlainString());
        }
        value = value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static AffinityScore of(BigDecimal value) {
        return new AffinityScore(value);
    }

    public static AffinityScore of(double value) {
        return new AffinityScore(BigDecimal.valueOf(value));
    }

    /**
     * Min-max normalisation against an observed range. A zero-width range means every candidate
     * scored the same, so the signal carries no ranking information and collapses to zero rather
     * than to an arbitrary constant.
     */
    public static AffinityScore normalize(BigDecimal raw, BigDecimal min, BigDecimal max) {
        if (raw == null || min == null || max == null) {
            return ZERO;
        }
        BigDecimal range = max.subtract(min);
        if (range.signum() <= 0) {
            return ZERO;
        }
        BigDecimal clamped = raw.max(min).min(max);
        return new AffinityScore(clamped.subtract(min).divide(range, SCALE, RoundingMode.HALF_UP));
    }

    public BigDecimal weightedBy(BigDecimal weight) {
        return value.multiply(weight);
    }

    public boolean isZero() {
        return value.signum() == 0;
    }

    @Override
    public int compareTo(AffinityScore other) {
        return value.compareTo(other.value);
    }
}
