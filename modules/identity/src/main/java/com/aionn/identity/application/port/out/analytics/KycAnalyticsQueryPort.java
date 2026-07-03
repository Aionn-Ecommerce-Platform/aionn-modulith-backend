package com.aionn.identity.application.port.out.analytics;

import com.aionn.identity.application.dto.analytics.result.KycAnalyticsResult;

import java.time.LocalDate;

public interface KycAnalyticsQueryPort {

    KycAnalyticsResult getKycAnalytics(LocalDate from, LocalDate to);
}
