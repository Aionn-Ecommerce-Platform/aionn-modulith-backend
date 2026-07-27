package com.aionn.notification.application.port.in.analytics;

import com.aionn.notification.application.dto.analytics.result.AnalyticsResult;

public interface GetCampaignAnalyticsInputPort {
    AnalyticsResult execute(String campaignId);
}