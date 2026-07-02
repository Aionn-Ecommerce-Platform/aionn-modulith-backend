package com.aionn.identity.application.port.in.feedback;

import com.aionn.identity.application.dto.common.PageResult;
import com.aionn.identity.application.dto.feedback.result.FeedbackResult;
import com.aionn.identity.domain.valueobject.FeedbackStatus;

public interface ListAdminFeedbackQueryPort {

    PageResult<FeedbackResult> execute(FeedbackStatus status, int page, int size);
}
