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
        BigDecimal originalPriceFrom,
        String currency,
        BigDecimal score,
        RecommendationReason reason,
        List<String> skuIds,
        List<String> categoryIds,
        String brandId,
        Double rating,
        Long reviewCount,
        Long soldCount) {

    public RecommendationItemResult(
            String productId,
            String name,
            String imageUrl,
            BigDecimal priceFrom,
            BigDecimal originalPriceFrom,
            String currency,
            BigDecimal score,
            RecommendationReason reason,
            List<String> skuIds) {
        this(productId, name, imageUrl, priceFrom, originalPriceFrom, currency, score, reason, skuIds,
                List.of(), null, null, null, 0L);
    }

    public RecommendationItemResult {
        skuIds = skuIds == null ? List.of() : List.copyOf(skuIds);
        categoryIds = categoryIds == null ? List.of() : List.copyOf(categoryIds);
    }

    public RecommendationItemResult withReason(RecommendationReason newReason) {
        return new RecommendationItemResult(
                productId, name, imageUrl, priceFrom, originalPriceFrom, currency, score,
                newReason, skuIds, categoryIds, brandId, rating, reviewCount, soldCount);
    }
}
