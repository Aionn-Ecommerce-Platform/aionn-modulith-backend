package com.aionn.promotion.adapter.rest.dto.campaign.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CampaignResponse(
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
