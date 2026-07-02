package com.aionn.identity.application.port.in.feedback;

import com.aionn.identity.application.dto.feedback.command.AdminFeedbackCommands;
import com.aionn.identity.application.dto.feedback.result.FeedbackResult;

public interface ChangeFeedbackStatusInputPort {

    FeedbackResult execute(AdminFeedbackCommands.ChangeFeedbackStatus command);
}
