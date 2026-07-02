package com.aionn.identity.application.port.in.feedback;

import com.aionn.identity.application.dto.feedback.command.AdminFeedbackCommands;
import com.aionn.identity.application.dto.feedback.result.FeedbackResult;

public interface ReplyFeedbackInputPort {

    FeedbackResult execute(AdminFeedbackCommands.ReplyFeedback command);
}
