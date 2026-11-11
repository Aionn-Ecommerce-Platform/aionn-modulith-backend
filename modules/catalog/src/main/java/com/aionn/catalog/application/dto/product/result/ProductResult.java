package com.aionn.catalog.application.dto.product.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ProductResult(
        String productId,
        String merchantId,
        String name,
        String brandId,
        List<String> categoryIds,
        List<String> imageList,
        List<String> tags,
        Map<String, String> attributes,
        List<Variant> variants,
        String aiDescription,
        String status,
        Instant createdAt,
        Instant updatedAt,
        long soldCount,
        boolean isBestSeller,
        Double rating,
        Long reviewCount,
        String slug) {

    public ProductResult(
            String productId,
            String merchantId,
            String name,
            String brandId,
            List<String> categoryIds,
            List<String> imageList,
            List<String> tags,
            Map<String, String> attributes,
            List<Variant> variants,
            String aiDescription,
            String status,
            Instant createdAt,
            Instant updatedAt) {
        this(productId, merchantId, name, brandId, categoryIds, imageList, tags,
                attributes, variants, aiDescription, status, createdAt, updatedAt, 0L, false, 0.0, 0L, null);
    }

    public ProductResult(
            String productId,
            String merchantId,
            String name,
            String brandId,
            List<String> categoryIds,
            List<String> imageList,
            List<String> tags,
            Map<String, String> attributes,
            List<Variant> variants,
            String aiDescription,
            String status,
            Instant createdAt,
            Instant updatedAt,
            long soldCount) {
        this(productId, merchantId, name, brandId, categoryIds, imageList, tags,
                attributes, variants, aiDescription, status, createdAt, updatedAt, soldCount, false, 0.0, 0L, null);
    }

    public ProductResult(
            String productId,
            String merchantId,
            String name,
            String brandId,
            List<String> categoryIds,
            List<String> imageList,
            List<String> tags,
            Map<String, String> attributes,
            List<Variant> variants,
            String aiDescription,
            String status,
            Instant createdAt,
            Instant updatedAt,
            long soldCount,
            boolean isBestSeller) {
        this(productId, merchantId, name, brandId, categoryIds, imageList, tags,
                attributes, variants, aiDescription, status, createdAt, updatedAt, soldCount, isBestSeller, 0.0, 0L, null);
    }

    public record Variant(String skuId, Map<String, String> attributeValues, BigDecimal price,
            BigDecimal originalPrice, String currency, Integer weightGram, BigDecimal lengthCm,
            BigDecimal widthCm, BigDecimal heightCm) {
    }
}
