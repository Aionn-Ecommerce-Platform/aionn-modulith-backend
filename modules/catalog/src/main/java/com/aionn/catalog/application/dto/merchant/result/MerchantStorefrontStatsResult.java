package com.aionn.catalog.application.dto.merchant.result;

import java.time.Instant;

public record MerchantStorefrontStatsResult(
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
