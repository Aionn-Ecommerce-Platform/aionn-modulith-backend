package com.aionn.recommendation.application.dto.product;

import java.math.BigDecimal;
import java.util.List;

/**
 * Product facts required to score and hydrate a recommendation slate.
 *
 * <p>Kept as an application DTO rather than a port-nested record because it is
 * shared by several application services and adapters.
 */
public record ProductAttributes(
        String productId,
        String name,
        String brandId,
        List<String> categoryIds,
        List<String> skuIds,
        String imageUrl,
        BigDecimal priceFrom,
        BigDecimal originalPriceFrom,
        String currency,
        Double rating,
        Long reviewCount,
        Long soldCount,
        String provinceCode,
        String provinceName) {

    public ProductAttributes(
            String productId,
            String name,
            String brandId,
            List<String> categoryIds,
            List<String> skuIds,
            String imageUrl,
            BigDecimal priceFrom,
            BigDecimal originalPriceFrom,
            String currency) {
        this(productId, name, brandId, categoryIds, skuIds, imageUrl, priceFrom,
                originalPriceFrom, currency, null, null, 0L, null, null);
    }

    public ProductAttributes(
            String productId,
            String name,
            String brandId,
            List<String> categoryIds,
            List<String> skuIds,
            String imageUrl,
            BigDecimal priceFrom,
            BigDecimal originalPriceFrom,
            String currency,
            Double rating,
            Long reviewCount,
            Long soldCount) {
        this(productId, name, brandId, categoryIds, skuIds, imageUrl, priceFrom,
                originalPriceFrom, currency, rating, reviewCount, soldCount, null, null);
    }

    public ProductAttributes {
        categoryIds = categoryIds == null ? List.of() : List.copyOf(categoryIds);
        skuIds = skuIds == null ? List.of() : List.copyOf(skuIds);
    }
}
