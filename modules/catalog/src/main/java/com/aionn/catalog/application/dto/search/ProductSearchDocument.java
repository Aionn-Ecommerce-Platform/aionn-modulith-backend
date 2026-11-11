package com.aionn.catalog.application.dto.search;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ProductSearchDocument(
        String productId,
        String merchantId,
        String name,
        String aiDescription,
        String brandId,
        List<String> categoryIds,
        List<String> collectionIds,
        List<String> tags,
        List<String> imageList,
        Map<String, String> filterableAttributes,
        BigDecimal priceFrom,
        BigDecimal priceTo,
        String currency,
        Boolean hasDiscount,
        String status,
        Instant updatedAt,
        Double rating,
        Long soldCount) {
}
