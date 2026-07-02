package com.aionn.identity.application.dto.feedback.command;

import com.aionn.sharedkernel.application.command.Command;

public record SubmitFeedbackCommand(
        String userId,
        String category,
        String subject,
        String content,
        Integer rating,
        String contactEmail,
        String contactPhone) implements Command {
}
