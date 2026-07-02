package com.aionn.identity.application.port.in.feedback;

import com.aionn.identity.application.dto.analytics.result.FeedbackAnalyticsResult;

import java.time.LocalDate;

public interface GetFeedbackAnalyticsQueryPort {

    FeedbackAnalyticsResult execute(LocalDate from, LocalDate to);
}
