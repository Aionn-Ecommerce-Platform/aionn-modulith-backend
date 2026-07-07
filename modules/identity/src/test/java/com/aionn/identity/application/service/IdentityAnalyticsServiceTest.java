package com.aionn.identity.application.service;

import com.aionn.identity.application.dto.analytics.result.FeedbackAnalyticsResult;
import com.aionn.identity.application.dto.analytics.result.KycAnalyticsResult;
import com.aionn.identity.application.dto.analytics.result.UserAnalyticsResult;
import com.aionn.identity.application.port.out.analytics.FeedbackAnalyticsQueryPort;
import com.aionn.identity.application.port.out.analytics.KycAnalyticsQueryPort;
import com.aionn.identity.application.port.out.analytics.UserAnalyticsQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityAnalyticsServiceTest {

    private static final LocalDate FROM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TO = LocalDate.of(2026, 1, 31);

    @Mock
    private UserAnalyticsQueryPort userAnalyticsQueryPort;
    @Mock
    private KycAnalyticsQueryPort kycAnalyticsQueryPort;
    @Mock
    private FeedbackAnalyticsQueryPort feedbackAnalyticsQueryPort;

    private IdentityAnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new IdentityAnalyticsService(
                userAnalyticsQueryPort, kycAnalyticsQueryPort, feedbackAnalyticsQueryPort);
    }

    @Test
    void getUserAnalyticsDelegatesToPort() {
        UserAnalyticsResult expected = new UserAnalyticsResult(
                FROM, TO, 100, 10, List.of(), List.of(), List.of());
        when(userAnalyticsQueryPort.getUserAnalytics(FROM, TO)).thenReturn(expected);

        assertSame(expected, service.getUserAnalytics(FROM, TO));
    }

    @Test
    void getKycAnalyticsDelegatesToPort() {
        KycAnalyticsResult expected = new KycAnalyticsResult(FROM, TO, 1, 2, 3, 6, 0.5, 12.0);
        when(kycAnalyticsQueryPort.getKycAnalytics(FROM, TO)).thenReturn(expected);

        assertSame(expected, service.getKycAnalytics(FROM, TO));
    }

    @Test
    void getFeedbackAnalyticsDelegatesToPort() {
        FeedbackAnalyticsResult expected = new FeedbackAnalyticsResult(
                FROM, TO, 4, 5, 1, 8.0, List.of());
        when(feedbackAnalyticsQueryPort.getFeedbackAnalytics(FROM, TO)).thenReturn(expected);

        assertSame(expected, service.getFeedbackAnalytics(FROM, TO));
    }
}
