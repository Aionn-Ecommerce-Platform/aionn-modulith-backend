package com.aionn.catalog.application.dto.review.query;

import com.aionn.sharedkernel.application.query.Query;

public record GetProductRatingSummaryQuery(String productId) implements Query {
}
