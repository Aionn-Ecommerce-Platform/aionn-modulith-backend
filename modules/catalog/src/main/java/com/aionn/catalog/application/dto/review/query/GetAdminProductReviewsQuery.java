package com.aionn.catalog.application.dto.review.query;

import com.aionn.sharedkernel.application.query.Query;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;

public record GetAdminProductReviewsQuery(String productId, OffsetPagination pagination) implements Query {
}
