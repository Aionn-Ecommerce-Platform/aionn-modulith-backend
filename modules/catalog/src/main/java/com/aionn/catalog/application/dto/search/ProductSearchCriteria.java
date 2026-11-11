package com.aionn.catalog.application.dto.search;

import com.aionn.catalog.domain.valueobject.ProductStatus;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
        Double ratingMin,
        Boolean onSale,
        List<String> collectionIds) {

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
        collectionIds = collectionIds == null ? List.of() : List.copyOf(collectionIds);
        if (sort == null) {
            sort = Sort.RELEVANCE;
        }
        if (page < 0) {
            page = 0;
        }
        if (size < 1) {
            size = OffsetPagination.DEFAULT_SIZE;
        }
        if (size > OffsetPagination.MAX_SIZE) {
            size = OffsetPagination.MAX_SIZE;
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
        this(q, merchantId, status, categoryIds, brandIds, priceMin, priceMax, attributes, sort, page, size,
                null, null, List.of());
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
            int size,
            Double ratingMin) {
        this(q, merchantId, status, categoryIds, brandIds, priceMin, priceMax, attributes, sort, page, size,
                ratingMin, null, List.of());
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
            int size,
            Double ratingMin,
            Boolean onSale) {
        this(q, merchantId, status, categoryIds, brandIds, priceMin, priceMax, attributes, sort, page, size,
                ratingMin, onSale, List.of());
    }

    public boolean hasText() {
        return q != null && !q.isBlank();
    }
}
