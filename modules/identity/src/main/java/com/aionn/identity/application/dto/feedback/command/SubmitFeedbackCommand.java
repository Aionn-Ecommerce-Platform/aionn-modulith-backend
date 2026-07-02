package com.aionn.identity.application.dto.feedback.command;

public record SubmitFeedbackCommand(
        String userId,
        String category,
        String subject,
        String content,
        Integer rating,
        String contactEmail,
        String contactPhone) {
}
