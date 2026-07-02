package com.aionn.identity.application.dto.feedback.result;

import java.time.LocalDateTime;

public record FeedbackResult(
        String feedbackId,
        String userId,
        String category,
        String subject,
        String content,
        Integer rating,
        String contactEmail,
        String contactPhone,
        String status,
        String handledBy,
        LocalDateTime handledAt,
        String adminReply,
        LocalDateTime createdAt) {
}
