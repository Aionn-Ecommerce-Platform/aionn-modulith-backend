package com.aionn.catalog.application.dto.merchant.query;

import com.aionn.sharedkernel.domain.vo.OffsetPagination;

public record ListMerchantsQuery(OffsetPagination pagination) {
}
