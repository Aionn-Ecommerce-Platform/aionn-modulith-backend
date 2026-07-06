package com.aionn.catalog.application.dto.product.query;

import com.aionn.sharedkernel.application.query.Query;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;

public record SearchProductsQuery(String keyword, OffsetPagination pagination) implements Query {
}
