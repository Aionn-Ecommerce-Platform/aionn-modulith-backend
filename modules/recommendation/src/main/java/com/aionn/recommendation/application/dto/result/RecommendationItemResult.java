package com.aionn.recommendation.application.dto.result;

import com.aionn.recommendation.domain.valueobject.RecommendationReason;

import java.math.BigDecimal;
import java.util.List;

/**
 * A ranked, hydrated recommendation.
 *
 * @param skuIds every variant of the product. Carried so availability filtering can run after the
 *               cache without a second catalog round trip; the REST mapper does not expose it, since
 *               clients recommend products, not SKUs.
 */
public record RecommendationItemResult(
        String productId,
        String name,
        String imageUrl,
        BigDecimal priceFrom,
        String currency,
        BigDecimal score,
        RecommendationReason reason,
        List<String> skuIds) {

    public RecommendationItemResult {
        skuIds = skuIds == null ? List.of() : List.copyOf(skuIds);
    }
}
