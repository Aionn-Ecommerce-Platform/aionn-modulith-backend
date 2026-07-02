package com.aionn.identity.application.dto.analytics.result;

import java.time.LocalDate;
import java.util.List;

public record FeedbackAnalyticsResult(
        LocalDate from,
        LocalDate to,
        long open,
        long resolved,
        long otherActive,
        double avgResolutionHours,
        List<CategoryCount> byCategory) {

    public record CategoryCount(String category, long count) {
    }
}
