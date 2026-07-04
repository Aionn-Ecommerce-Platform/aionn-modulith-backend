package com.aionn.identity.adapter.rest.dto.feedback.response;

import java.time.LocalDate;
import java.util.List;

public record FeedbackAnalyticsResponse(
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
