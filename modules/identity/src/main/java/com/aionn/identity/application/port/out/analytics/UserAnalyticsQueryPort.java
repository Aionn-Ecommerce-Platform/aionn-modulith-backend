package com.aionn.identity.application.port.out.analytics;

import com.aionn.identity.application.dto.analytics.result.UserAnalyticsResult;

import java.time.LocalDate;

public interface UserAnalyticsQueryPort {

    UserAnalyticsResult getUserAnalytics(LocalDate from, LocalDate to);
}
