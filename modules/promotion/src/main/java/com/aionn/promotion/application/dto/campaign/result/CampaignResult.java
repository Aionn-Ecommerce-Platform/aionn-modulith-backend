package com.aionn.promotion.application.dto.campaign.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CampaignResult(
        String campaignId,
        String name,
        String type,
        BigDecimal budget,
        BigDecimal budgetRemaining,
        String currency,
        Instant startDate,
        Instant endDate,
        String createdBy,
        String status,
        BigDecimal minOrderValue,
        List<String> applicableCategoryIds,
        Integer maxClaimsPerUser,
        Integer maxUsesPerVoucher,
        Instant createdAt,
        Instant updatedAt) {
}

