package com.aionn.recommendation.adapter.rest.dto.recommendation.response;

import com.aionn.recommendation.domain.valueobject.RecommendationReason;

import java.math.BigDecimal;

/**
 * @param reason machine-readable enum, not a sentence. Clients localise the wording, so an English
 *               phrase produced here could not be translated.
 */
public record RecommendationResponse(
        String productId,
        String name,
        String imageUrl,
        BigDecimal priceFrom,
        String currency,
        BigDecimal score,
        RecommendationReason reason) {
}
