package com.aionn.identity.application.service;

import com.aionn.identity.application.dto.analytics.result.FeedbackAnalyticsResult;
import com.aionn.identity.application.dto.analytics.result.KycAnalyticsResult;
import com.aionn.identity.application.dto.analytics.result.UserAnalyticsResult;
import com.aionn.identity.application.port.out.analytics.FeedbackAnalyticsQueryPort;
import com.aionn.identity.application.port.out.analytics.KycAnalyticsQueryPort;
import com.aionn.identity.application.port.out.analytics.UserAnalyticsQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class IdentityAnalyticsService {

    private final UserAnalyticsQueryPort userAnalyticsQueryPort;
    private final KycAnalyticsQueryPort kycAnalyticsQueryPort;
    private final FeedbackAnalyticsQueryPort feedbackAnalyticsQueryPort;

    @Transactional(readOnly = true)
    public UserAnalyticsResult getUserAnalytics(LocalDate from, LocalDate to) {
        return userAnalyticsQueryPort.getUserAnalytics(from, to);
    }

    @Transactional(readOnly = true)
    public KycAnalyticsResult getKycAnalytics(LocalDate from, LocalDate to) {
        return kycAnalyticsQueryPort.getKycAnalytics(from, to);
    }

    @Transactional(readOnly = true)
    public FeedbackAnalyticsResult getFeedbackAnalytics(LocalDate from, LocalDate to) {
        return feedbackAnalyticsQueryPort.getFeedbackAnalytics(from, to);
    }
}
