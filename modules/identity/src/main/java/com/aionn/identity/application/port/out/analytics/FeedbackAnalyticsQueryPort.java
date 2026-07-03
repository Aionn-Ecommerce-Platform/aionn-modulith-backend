package com.aionn.identity.application.port.out.analytics;

import com.aionn.identity.application.dto.analytics.result.FeedbackAnalyticsResult;

import java.time.LocalDate;

public interface FeedbackAnalyticsQueryPort {

    FeedbackAnalyticsResult getFeedbackAnalytics(LocalDate from, LocalDate to);
}
