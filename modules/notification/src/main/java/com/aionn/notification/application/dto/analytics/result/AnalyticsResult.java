package com.aionn.notification.application.dto.analytics.result;

import java.time.Instant;

public record AnalyticsResult(
        String reportId,
        String campaignId,
        int sentCount,
        int readCount,
        int failedCount,
        Instant generatedAt) {
}

