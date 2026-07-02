package com.aionn.identity.application.port.in.feedback;

import com.aionn.identity.application.dto.feedback.result.FeedbackResult;

import java.util.List;

public interface ListMyFeedbackQueryPort {

    List<FeedbackResult> execute(String userId);
}
