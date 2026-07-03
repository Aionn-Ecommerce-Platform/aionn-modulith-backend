package com.aionn.identity.application.usecase.feedback;

import com.aionn.identity.application.dto.feedback.command.AdminFeedbackCommands;
import com.aionn.identity.application.dto.feedback.result.FeedbackResult;
import com.aionn.identity.application.mapper.FeedbackResultMapper;
import com.aionn.identity.application.port.in.feedback.ReplyFeedbackInputPort;
import com.aionn.identity.application.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReplyFeedbackUseCase implements ReplyFeedbackInputPort {

    private final FeedbackService feedbackService;
    private final FeedbackResultMapper feedbackResultMapper;

    @Override
    @Transactional
    public FeedbackResult execute(AdminFeedbackCommands.ReplyFeedback command) {
        return feedbackResultMapper.toResult(feedbackService.adminReply(
                command.feedbackId(), command.adminId(), command.reply(), command.newStatus()));
    }
}
