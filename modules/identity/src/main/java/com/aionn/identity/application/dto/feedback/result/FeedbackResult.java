package com.aionn.identity.application.dto.feedback.result;

import java.time.Instant;

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
        Instant handledAt,
        String adminReply,
        Instant createdAt) {
}
