package com.aionn.catalog.application.dto.analytics.result;

import java.util.List;

public record ProductAnalyticsResult(
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
