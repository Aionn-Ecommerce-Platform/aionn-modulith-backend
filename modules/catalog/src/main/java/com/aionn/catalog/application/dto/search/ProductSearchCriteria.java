package com.aionn.catalog.application.dto.search;

import com.aionn.catalog.domain.valueobject.ProductStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

// Read-side filter for the faceted catalog search. Backed by the
// ProductSearchIndex port (OpenSearch in production, in-memory otherwise).
public record ProductSearchCriteria(
        String q,
        String merchantId,
        ProductStatus status,
        List<String> categoryIds,
        List<String> brandIds,
        BigDecimal priceMin,
        BigDecimal priceMax,
        Map<String, List<String>> attributes,
        Sort sort,
        int page,
        int size,
        Double ratingMin) {

    public enum Sort {
        RELEVANCE,
        NEWEST,
        PRICE_ASC,
        PRICE_DESC,
        BEST_SELLER
    }

    public ProductSearchCriteria {
        categoryIds = categoryIds == null ? List.of() : List.copyOf(categoryIds);
        brandIds = brandIds == null ? List.of() : List.copyOf(brandIds);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        if (sort == null) {
            sort = Sort.RELEVANCE;
        }
        if (page < 0) {
            page = 0;
        }
        if (size < 1) {
            size = 20;
        }
        if (size > 100) {
            size = 100;
        }
    }

    public ProductSearchCriteria(
            String q,
            String merchantId,
            ProductStatus status,
            List<String> categoryIds,
            List<String> brandIds,
            BigDecimal priceMin,
            BigDecimal priceMax,
            Map<String, List<String>> attributes,
            Sort sort,
            int page,
            int size) {
        this(q, merchantId, status, categoryIds, brandIds, priceMin, priceMax, attributes, sort, page, size, null);
    }

    public boolean hasText() {
        return q != null && !q.isBlank();
    }
}
