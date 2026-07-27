package com.aionn.notification.application.usecase.analytics;

import com.aionn.notification.application.dto.analytics.result.AnalyticsResult;
import com.aionn.notification.application.port.in.analytics.GetCampaignAnalyticsInputPort;
import com.aionn.notification.application.service.NotificationAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetCampaignAnalyticsUseCase implements GetCampaignAnalyticsInputPort {

    private final NotificationAnalyticsService analyticsService;

    @Override
    @Transactional
    public AnalyticsResult execute(String campaignId) {
        return analyticsService.report(campaignId);
    }
}