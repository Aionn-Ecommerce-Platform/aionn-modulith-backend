package com.aionn.identity.application.port.in.feedback;

import com.aionn.identity.application.dto.feedback.command.SubmitFeedbackCommand;
import com.aionn.identity.application.dto.feedback.result.FeedbackResult;

public interface SubmitFeedbackInputPort {

    FeedbackResult execute(SubmitFeedbackCommand command);
}
