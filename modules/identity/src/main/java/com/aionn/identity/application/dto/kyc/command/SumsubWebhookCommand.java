package com.aionn.identity.application.dto.kyc.command;

public record SumsubWebhookCommand(
        byte[] payload,
        String digest,
        String digestAlgorithm,
        String providerApplicantId,
        String providerReviewStatus,
        String reviewAnswer,
        String moderationComment,
        String clientComment,
        String correlationId) {
}
