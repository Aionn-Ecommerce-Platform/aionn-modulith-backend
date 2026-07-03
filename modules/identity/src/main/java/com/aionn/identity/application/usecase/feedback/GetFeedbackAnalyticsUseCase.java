package com.aionn.identity.application.usecase.feedback;

import com.aionn.identity.application.dto.analytics.result.FeedbackAnalyticsResult;
import com.aionn.identity.application.port.in.feedback.GetFeedbackAnalyticsQueryPort;
import com.aionn.identity.application.port.out.analytics.FeedbackAnalyticsQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class GetFeedbackAnalyticsUseCase implements GetFeedbackAnalyticsQueryPort {

    private final FeedbackAnalyticsQueryPort feedbackAnalyticsQueryPort;

    @Override
    @Transactional(readOnly = true)
    public FeedbackAnalyticsResult execute(LocalDate from, LocalDate to) {
        return feedbackAnalyticsQueryPort.getFeedbackAnalytics(from, to);
    }
}
