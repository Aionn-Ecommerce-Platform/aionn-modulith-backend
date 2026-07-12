package com.aionn.identity.adapter.rest.dto.feedback.response;

import java.time.Instant;

public record FeedbackResponse(
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
