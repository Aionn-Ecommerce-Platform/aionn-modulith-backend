package com.aionn.catalog.application.dto.review.result;

import java.time.Instant;
import java.util.List;

public record ReviewResult(
        String reviewId,
        String productId,
        String userId,
        String orderId,
        int rating,
        String title,
        String content,
        List<String> imageUrls,
        String status,
        String merchantReply,
        Instant merchantRepliedAt,
        String reportedByMerchantId,
        String reportReason,
        Instant reportedAt,
        Instant createdAt,
        Instant updatedAt) {
}
