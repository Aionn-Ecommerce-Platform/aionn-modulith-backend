package com.aionn.catalog.adapter.rest.dto.product.response;

import com.aionn.catalog.application.dto.common.PageResult;

import java.math.BigDecimal;
import java.util.Map;

public record ProductSearchResponse(
        PageResult<ProductResponse> page,
        Facets facets) {

    public record Facets(
            Map<String, Long> brands,
            Map<String, Long> categories,
            Map<String, Map<String, Long>> attributes,
            PriceRange priceRange) {
    }

    public record PriceRange(BigDecimal min, BigDecimal max) {
    }
}
