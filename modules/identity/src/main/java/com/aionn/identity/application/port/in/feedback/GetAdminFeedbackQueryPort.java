package com.aionn.identity.application.port.in.feedback;

import com.aionn.identity.application.dto.feedback.result.FeedbackResult;

public interface GetAdminFeedbackQueryPort {

    FeedbackResult execute(String feedbackId);
}
