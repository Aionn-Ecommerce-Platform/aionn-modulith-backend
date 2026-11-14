package com.aionn.catalog.adapter.rest.dto.product.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Query parameters accepted by the faceted catalogue search endpoint. Kept as a transport DTO so the
 * controller only binds HTTP input and the criteria assembly stays testable on its own.
 */
public record SearchCatalogRequest(
        String q,
        String merchantId,
        List<String> categoryIds,
        List<String> brandIds,
        BigDecimal priceMin,
        BigDecimal priceMax,
        Double ratingMin,
        Boolean onSale,
        List<String> collectionIds,
        String sort,
        int page,
        int size,
        Map<String, String> allParams) {
}
