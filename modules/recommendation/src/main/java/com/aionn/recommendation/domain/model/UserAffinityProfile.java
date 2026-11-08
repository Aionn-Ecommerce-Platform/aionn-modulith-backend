package com.aionn.recommendation.domain.model;

import com.aionn.recommendation.domain.exception.RecommendationErrorCode;
import com.aionn.recommendation.domain.exception.RecommendationException;
import com.aionn.recommendation.domain.valueobject.AffinityScore;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A user's aggregated taste, rebuilt wholesale by the profile refresh job (decay makes incremental
 * updates stale). The price band captures a signal browsing history misses: a flagship phone is a poor
 * recommendation for someone who only buys sub-500k accessories.
 */
@Getter
public class UserAffinityProfile {

    private final String userId;
    private final Map<String, AffinityScore> categoryAffinities;
    private final Map<String, AffinityScore> brandAffinities;
    private final BigDecimal priceBandMin;
    private final BigDecimal priceBandMax;
    private final int interactionCount;
    private final Instant lastInteractionAt;

    public UserAffinityProfile(
            String userId,
            Map<String, AffinityScore> categoryAffinities,
            Map<String, AffinityScore> brandAffinities,
            BigDecimal priceBandMin,
            BigDecimal priceBandMax,
            int interactionCount,
            Instant lastInteractionAt) {
        if (userId == null || userId.isBlank()) {
            throw new RecommendationException(RecommendationErrorCode.INVALID_ARGUMENT,
                    "userId must not be blank");
        }
        if (interactionCount < 0) {
            throw new RecommendationException(RecommendationErrorCode.INVALID_ARGUMENT,
                    "interactionCount must not be negative");
        }
        if (priceBandMin != null && priceBandMax != null && priceBandMin.compareTo(priceBandMax) > 0) {
            throw new RecommendationException(RecommendationErrorCode.INVALID_ARGUMENT,
                    "priceBandMin must not exceed priceBandMax");
        }
        this.userId = userId.trim();
        this.categoryAffinities = copyOf(categoryAffinities);
        this.brandAffinities = copyOf(brandAffinities);
        this.priceBandMin = priceBandMin;
        this.priceBandMax = priceBandMax;
        this.interactionCount = interactionCount;
        this.lastInteractionAt = lastInteractionAt;
    }

    public static UserAffinityProfile empty(String userId) {
        return new UserAffinityProfile(userId, Map.of(), Map.of(), null, null, 0, null);
    }

    public boolean hasNoSignal() {
        return categoryAffinities.isEmpty() && brandAffinities.isEmpty();
    }

    public AffinityScore categoryAffinity(String categoryId) {
        return categoryAffinities.getOrDefault(categoryId, AffinityScore.ZERO);
    }

    public AffinityScore brandAffinity(String brandId) {
        return brandId == null ? AffinityScore.ZERO
                : brandAffinities.getOrDefault(brandId, AffinityScore.ZERO);
    }

    /** A product in several categories is judged by its strongest link, not diluted by the weakest. */
    public AffinityScore bestCategoryAffinity(Iterable<String> productCategoryIds) {
        if (productCategoryIds == null) {
            return AffinityScore.ZERO;
        }
        AffinityScore best = AffinityScore.ZERO;
        for (String categoryId : productCategoryIds) {
            AffinityScore candidate = categoryAffinity(categoryId);
            if (candidate.compareTo(best) > 0) {
                best = candidate;
            }
        }
        return best;
    }

    /** 1 inside the band, tapering to 0 one band-width away. A missing price scores zero, not a penalty. */
    public AffinityScore priceFit(BigDecimal price) {
        if (price == null || priceBandMin == null || priceBandMax == null) {
            return AffinityScore.ZERO;
        }
        if (price.compareTo(priceBandMin) >= 0 && price.compareTo(priceBandMax) <= 0) {
            return AffinityScore.ONE;
        }
        BigDecimal bandWidth = priceBandMax.subtract(priceBandMin);
        BigDecimal reference = bandWidth.signum() > 0 ? bandWidth : priceBandMax.max(BigDecimal.ONE);
        BigDecimal distance = price.compareTo(priceBandMin) < 0
                ? priceBandMin.subtract(price)
                : price.subtract(priceBandMax);
        if (distance.compareTo(reference) >= 0) {
            return AffinityScore.ZERO;
        }
        return AffinityScore.normalize(reference.subtract(distance), BigDecimal.ZERO, reference);
    }

    public Optional<Instant> lastInteraction() {
        return Optional.ofNullable(lastInteractionAt);
    }

    private static Map<String, AffinityScore> copyOf(Map<String, AffinityScore> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
