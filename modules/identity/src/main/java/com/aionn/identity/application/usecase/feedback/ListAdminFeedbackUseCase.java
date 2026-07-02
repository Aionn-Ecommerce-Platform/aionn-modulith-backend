package com.aionn.identity.application.usecase.feedback;

import com.aionn.identity.application.dto.common.PageResult;
import com.aionn.identity.application.dto.feedback.result.FeedbackResult;
import com.aionn.identity.application.mapper.FeedbackResultMapper;
import com.aionn.identity.application.port.in.feedback.ListAdminFeedbackQueryPort;
import com.aionn.identity.application.service.FeedbackService;
import com.aionn.identity.domain.valueobject.FeedbackStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListAdminFeedbackUseCase implements ListAdminFeedbackQueryPort {

    private final FeedbackService feedbackService;
    private final FeedbackResultMapper feedbackResultMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResult<FeedbackResult> execute(FeedbackStatus status, int page, int size) {
        var result = feedbackService.adminList(status, page, size);
        var items = result.content().stream()
                .map(feedbackResultMapper::toResult)
                .toList();
        return new PageResult<>(
                items,
                result.page(),
                result.size(),
                result.totalElements());
    }
}
