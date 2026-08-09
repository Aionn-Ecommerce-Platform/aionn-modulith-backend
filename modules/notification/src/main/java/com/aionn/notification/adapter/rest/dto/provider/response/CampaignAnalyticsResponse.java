package com.aionn.notification.adapter.rest.dto.provider.response;

import java.time.Instant;

public record CampaignAnalyticsResponse(
        String reportId, String campaignId, int sentCount, int readCount,
        int failedCount, Instant generatedAt) {}
