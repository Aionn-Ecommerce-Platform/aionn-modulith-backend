package com.aionn.identity.application.dto.feedback.command;

import com.aionn.identity.domain.valueobject.FeedbackStatus;

public final class AdminFeedbackCommands {

    private AdminFeedbackCommands() {
    }

    public record ReplyFeedback(
            String feedbackId,
            String adminId,
            String reply,
            FeedbackStatus newStatus) {
    }

    public record ChangeFeedbackStatus(
            String feedbackId,
            String adminId,
            FeedbackStatus status) {
    }
}
