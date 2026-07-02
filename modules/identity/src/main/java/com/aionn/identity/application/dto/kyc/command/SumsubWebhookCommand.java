package com.aionn.identity.application.dto.kyc.command;

import com.aionn.sharedkernel.application.command.Command;

import java.util.Arrays;
import java.util.Objects;

public record SumsubWebhookCommand(
        byte[] payload,
        String digest,
        String digestAlgorithm,
        String providerApplicantId,
        String providerReviewStatus,
        String reviewAnswer,
        String moderationComment,
        String clientComment,
        String correlationId) implements Command {
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SumsubWebhookCommand(
                byte[] otherPayload,
                String otherDigest,
                String otherDigestAlgorithm,
                String otherProviderApplicantId,
                String otherProviderReviewStatus,
                String otherReviewAnswer,
                String otherModerationComment,
                String otherClientComment,
                String otherCorrelationId))) {
            return false;
        }
        return Arrays.equals(payload, otherPayload)
                && Objects.equals(digest, otherDigest)
                && Objects.equals(digestAlgorithm, otherDigestAlgorithm)
                && Objects.equals(providerApplicantId, otherProviderApplicantId)
                && Objects.equals(providerReviewStatus, otherProviderReviewStatus)
                && Objects.equals(reviewAnswer, otherReviewAnswer)
                && Objects.equals(moderationComment, otherModerationComment)
                && Objects.equals(clientComment, otherClientComment)
                && Objects.equals(correlationId, otherCorrelationId);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(
                digest,
                digestAlgorithm,
                providerApplicantId,
                providerReviewStatus,
                reviewAnswer,
                moderationComment,
                clientComment,
                correlationId);
        result = 31 * result + Arrays.hashCode(payload);
        return result;
    }

    @Override
    public String toString() {
        return "SumsubWebhookCommand[payloadLength=%s, digest=***, digestAlgorithm=%s, providerApplicantId=%s, providerReviewStatus=%s, reviewAnswer=%s, moderationComment=%s, clientComment=%s, correlationId=%s]"
                .formatted(
                        payload == null ? 0 : payload.length,
                        digestAlgorithm,
                        providerApplicantId,
                        providerReviewStatus,
                        reviewAnswer,
                        moderationComment,
                        clientComment,
                        correlationId);
    }
}
