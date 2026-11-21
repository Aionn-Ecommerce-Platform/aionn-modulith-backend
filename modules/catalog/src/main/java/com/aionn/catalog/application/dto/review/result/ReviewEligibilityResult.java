package com.aionn.catalog.application.dto.review.result;

public record ReviewEligibilityResult(boolean canReview, String reason, String orderId, String skuId) {
}
