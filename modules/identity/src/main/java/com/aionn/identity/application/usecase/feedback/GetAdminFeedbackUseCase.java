package com.aionn.identity.application.usecase.feedback;

import com.aionn.identity.application.dto.feedback.result.FeedbackResult;
import com.aionn.identity.application.mapper.FeedbackResultMapper;
import com.aionn.identity.application.port.in.feedback.GetAdminFeedbackQueryPort;
import com.aionn.identity.application.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetAdminFeedbackUseCase implements GetAdminFeedbackQueryPort {

    private final FeedbackService feedbackService;
    private final FeedbackResultMapper feedbackResultMapper;

    @Override
    @Transactional(readOnly = true)
    public FeedbackResult execute(String feedbackId) {
        return feedbackResultMapper.toResult(feedbackService.adminGet(feedbackId));
    }
}
