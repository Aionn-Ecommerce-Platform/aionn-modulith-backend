package com.aionn.identity.application.usecase.feedback;

import com.aionn.identity.application.dto.feedback.command.SubmitFeedbackCommand;
import com.aionn.identity.application.dto.feedback.result.FeedbackResult;
import com.aionn.identity.application.mapper.FeedbackResultMapper;
import com.aionn.identity.application.port.in.feedback.SubmitFeedbackInputPort;
import com.aionn.identity.application.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubmitFeedbackUseCase implements SubmitFeedbackInputPort {

    private final FeedbackService feedbackService;
    private final FeedbackResultMapper feedbackResultMapper;

    @Override
    @Transactional
    public FeedbackResult execute(SubmitFeedbackCommand command) {
        return feedbackResultMapper.toResult(feedbackService.submit(
                command.userId(),
                command.category(),
                command.subject(),
                command.content(),
                command.rating(),
                command.contactEmail(),
                command.contactPhone()));
    }
}
