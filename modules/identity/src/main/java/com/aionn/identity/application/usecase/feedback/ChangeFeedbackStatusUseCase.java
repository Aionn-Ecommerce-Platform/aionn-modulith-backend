package com.aionn.identity.application.usecase.feedback;

import com.aionn.identity.application.dto.feedback.command.AdminFeedbackCommands;
import com.aionn.identity.application.dto.feedback.result.FeedbackResult;
import com.aionn.identity.application.mapper.FeedbackResultMapper;
import com.aionn.identity.application.port.in.feedback.ChangeFeedbackStatusInputPort;
import com.aionn.identity.application.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChangeFeedbackStatusUseCase implements ChangeFeedbackStatusInputPort {

    private final FeedbackService feedbackService;
    private final FeedbackResultMapper feedbackResultMapper;

    @Override
    @Transactional
    public FeedbackResult execute(AdminFeedbackCommands.ChangeFeedbackStatus command) {
        return feedbackResultMapper.toResult(feedbackService.adminChangeStatus(
                command.feedbackId(), command.adminId(), command.status()));
    }
}
