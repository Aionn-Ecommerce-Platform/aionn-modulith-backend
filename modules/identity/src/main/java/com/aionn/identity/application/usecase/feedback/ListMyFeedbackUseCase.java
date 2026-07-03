package com.aionn.identity.application.usecase.feedback;

import com.aionn.identity.application.dto.feedback.result.FeedbackResult;
import com.aionn.identity.application.mapper.FeedbackResultMapper;
import com.aionn.identity.application.port.in.feedback.ListMyFeedbackQueryPort;
import com.aionn.identity.application.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListMyFeedbackUseCase implements ListMyFeedbackQueryPort {

    private final FeedbackService feedbackService;
    private final FeedbackResultMapper feedbackResultMapper;

    @Override
    @Transactional(readOnly = true)
    public List<FeedbackResult> execute(String userId) {
        return feedbackService.listMine(userId).stream()
                .map(feedbackResultMapper::toResult)
                .toList();
    }
}
