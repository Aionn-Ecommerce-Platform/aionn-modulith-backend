package com.aionn.catalog.application.dto.product.query;

import com.aionn.catalog.domain.valueobject.ProductStatus;
import com.aionn.sharedkernel.application.query.Query;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;

import java.math.BigDecimal;
import java.util.List;

public record ListProductsByMerchantQuery(
        String merchantId,
        OffsetPagination pagination,
        String search,
        ProductStatus status,
        List<String> categoryIds,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String sort) implements Query {

    public ListProductsByMerchantQuery(String merchantId, OffsetPagination pagination) {
        this(merchantId, pagination, null, null, List.of(), null, null, "NEWEST");
    }
}
