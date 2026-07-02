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
        if (!(other instanceof SumsubWebhookCommand that)) {
            return false;
        }
        return Arrays.equals(payload, that.payload)
                && Objects.equals(digest, that.digest)
                && Objects.equals(digestAlgorithm, that.digestAlgorithm)
                && Objects.equals(providerApplicantId, that.providerApplicantId)
                && Objects.equals(providerReviewStatus, that.providerReviewStatus)
                && Objects.equals(reviewAnswer, that.reviewAnswer)
                && Objects.equals(moderationComment, that.moderationComment)
                && Objects.equals(clientComment, that.clientComment)
                && Objects.equals(correlationId, that.correlationId);
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
