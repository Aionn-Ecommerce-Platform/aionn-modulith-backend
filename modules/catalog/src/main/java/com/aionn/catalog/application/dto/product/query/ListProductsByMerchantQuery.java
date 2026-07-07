package com.aionn.catalog.application.dto.product.query;

import com.aionn.sharedkernel.application.query.Query;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;

public record ListProductsByMerchantQuery(String merchantId, OffsetPagination pagination) implements Query {
}
