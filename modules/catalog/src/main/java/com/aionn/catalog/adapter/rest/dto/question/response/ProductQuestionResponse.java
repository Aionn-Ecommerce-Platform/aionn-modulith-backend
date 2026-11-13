package com.aionn.catalog.adapter.rest.dto.question.response;

import java.time.Instant;

public record ProductQuestionResponse(
        String questionId,
        String productId,
        String userId,
        String question,
        String answer,
        String answeredBy,
        Instant answeredAt,
        Instant createdAt,
        String userName,
        String userDisplayName,
        String userAvatarUrl) {
}
