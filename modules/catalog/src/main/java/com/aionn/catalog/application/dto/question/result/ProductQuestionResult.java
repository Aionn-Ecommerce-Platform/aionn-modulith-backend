package com.aionn.catalog.application.dto.question.result;

import java.time.Instant;

public record ProductQuestionResult(
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
