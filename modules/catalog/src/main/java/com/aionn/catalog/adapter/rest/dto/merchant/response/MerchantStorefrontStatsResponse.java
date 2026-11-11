package com.aionn.catalog.adapter.rest.dto.merchant.response;

import java.time.Instant;

public record MerchantStorefrontStatsResponse(
        long reviewCount,
        double averageRating,
        long productCount,
        long followerCount,
        Instant joinedAt,
        long conversations,
        long respondedConversations,
        Long medianFirstResponseSeconds,
        int responseRatePercent,
        String responseTimeBucket,
        boolean following) {
}
