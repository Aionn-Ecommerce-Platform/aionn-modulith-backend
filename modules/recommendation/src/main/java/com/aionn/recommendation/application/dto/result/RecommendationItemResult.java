package com.aionn.recommendation.application.dto.result;

import com.aionn.recommendation.domain.valueobject.RecommendationReason;

import java.math.BigDecimal;
import java.util.List;

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
        Long soldCount,
        String provinceCode,
        String provinceName) {

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
                List.of(), null, null, null, 0L, null, null);
    }

    public RecommendationItemResult {
        skuIds = skuIds == null ? List.of() : List.copyOf(skuIds);
        categoryIds = categoryIds == null ? List.of() : List.copyOf(categoryIds);
    }

    public RecommendationItemResult withReason(RecommendationReason newReason) {
        return new RecommendationItemResult(
                productId, name, imageUrl, priceFrom, originalPriceFrom, currency, score,
                newReason, skuIds, categoryIds, brandId, rating, reviewCount, soldCount,
                provinceCode, provinceName);
    }
}
