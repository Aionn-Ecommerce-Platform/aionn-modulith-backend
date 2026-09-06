package com.aionn.recommendation.domain.model;

import com.aionn.recommendation.domain.exception.RecommendationErrorCode;
import com.aionn.recommendation.domain.exception.RecommendationException;
import com.aionn.recommendation.domain.valueobject.AffinityScore;

import java.time.Instant;

/**
 * Cosine similarity between two products, derived from how often the same users interacted strongly
 * with both.
 *
 * @param coOccurrence number of users who interacted with both products; kept alongside the score
 *                     because a high cosine over two shared users is noise, and callers need to see
 *                     the difference.
 */
public record ItemSimilarity(
        String productId,
        String similarProductId,
        AffinityScore score,
        int coOccurrence,
        Instant computedAt) {

    public ItemSimilarity {
        productId = required(productId, "productId");
        similarProductId = required(similarProductId, "similarProductId");
        if (productId.equals(similarProductId)) {
            throw new RecommendationException(RecommendationErrorCode.SIMILARITY_INVALID,
                    "a product cannot be similar to itself");
        }
        if (score == null) {
            throw new RecommendationException(RecommendationErrorCode.SIMILARITY_INVALID,
                    "score must not be null");
        }
        if (coOccurrence <= 0) {
            throw new RecommendationException(RecommendationErrorCode.SIMILARITY_INVALID,
                    "coOccurrence must be positive");
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new RecommendationException(RecommendationErrorCode.SIMILARITY_INVALID,
                    field + " must not be blank");
        }
        return value.trim();
    }
}
