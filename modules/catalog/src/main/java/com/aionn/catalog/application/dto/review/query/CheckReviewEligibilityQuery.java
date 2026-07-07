package com.aionn.catalog.application.dto.review.query;

import com.aionn.sharedkernel.application.query.Query;

public record CheckReviewEligibilityQuery(String userId, String productId) implements Query {
}
