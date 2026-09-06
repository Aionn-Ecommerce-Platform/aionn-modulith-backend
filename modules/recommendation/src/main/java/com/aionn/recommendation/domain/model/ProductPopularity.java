package com.aionn.recommendation.domain.model;

import com.aionn.recommendation.domain.exception.RecommendationErrorCode;
import com.aionn.recommendation.domain.exception.RecommendationException;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Time-decayed popularity of a product.
 *
 * <p>Distinct from catalog's {@code product_sold_counters}, which is a lifetime total. This measures
 * recent momentum, so a product that sold well two years ago and nothing since ranks low here while
 * still being a legitimate best-seller in catalog.
 */
public record ProductPopularity(
        String productId,
        BigDecimal score,
        long viewCount,
        long purchaseCount,
        Instant computedAt) {

    public ProductPopularity {
        if (productId == null || productId.isBlank()) {
            throw new RecommendationException(RecommendationErrorCode.INVALID_ARGUMENT,
                    "productId must not be blank");
        }
        if (score == null || score.signum() < 0) {
            throw new RecommendationException(RecommendationErrorCode.INVALID_ARGUMENT,
                    "popularity score must not be negative");
        }
        if (viewCount < 0 || purchaseCount < 0) {
            throw new RecommendationException(RecommendationErrorCode.INVALID_ARGUMENT,
                    "counts must not be negative");
        }
        productId = productId.trim();
    }
}
