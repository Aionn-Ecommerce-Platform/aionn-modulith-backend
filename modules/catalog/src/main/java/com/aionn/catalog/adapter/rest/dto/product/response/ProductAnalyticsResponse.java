package com.aionn.catalog.adapter.rest.dto.product.response;

import java.util.List;

public record ProductAnalyticsResponse(
        long totalPublished,
        long totalDraft,
        long totalPendingReview,
        long totalArchived,
        long totalReviews,
        double averageRating,
        List<CategoryCount> topCategories) {

    public record CategoryCount(String categoryId, long count) {
    }
}
