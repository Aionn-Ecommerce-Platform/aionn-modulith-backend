package com.aionn.identity.application.dto.analytics.result;

import java.time.LocalDate;

public record KycAnalyticsResult(
        LocalDate from,
        LocalDate to,
        long pending,
        long approved,
        long rejected,
        long submitted,
        double approvalRate,
        double avgProcessingHours) {
}
