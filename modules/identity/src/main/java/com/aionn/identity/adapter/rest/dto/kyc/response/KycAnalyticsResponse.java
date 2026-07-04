package com.aionn.identity.adapter.rest.dto.kyc.response;

import java.time.LocalDate;

public record KycAnalyticsResponse(
        LocalDate from,
        LocalDate to,
        long pending,
        long approved,
        long rejected,
        long submitted,
        double approvalRate,
        double avgProcessingHours) {
}
