package com.aionn.recommendation.domain.model;

import com.aionn.recommendation.domain.exception.RecommendationErrorCode;
import com.aionn.recommendation.domain.exception.RecommendationException;
import com.aionn.recommendation.domain.valueobject.InteractionType;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One recorded behavioural signal. Immutable by design: the interaction log is
 * append-only, so an
 * interaction is never corrected in place - a later interaction supersedes it,
 * and decay handles the
 * fading of the earlier one.
 */
@Getter
public class UserInteraction {

    private final String interactionId;
    private final String userId;
    private final String productId;
    private final InteractionType type;
    private final BigDecimal weight;
    private final Instant occurredAt;

    /**
     * Identifier of the integration event that produced this signal, or null when the interaction was
     * written by something other than an event (seeded fixtures, a manual backfill).
     *
     * <p>This is what makes ingest idempotent. Outbox delivery is at-least-once, so the same event can
     * arrive twice; a unique index over this identifier and the signal's natural key lets the second
     * arrival be dropped by the database instead of inflating every score that sums over the log.
     */
    private final String sourceEventId;

    public UserInteraction(
            String interactionId,
            String userId,
            String productId,
            InteractionType type,
            BigDecimal weight,
            Instant occurredAt) {
        this(interactionId, userId, productId, type, weight, occurredAt, null);
    }

    public UserInteraction(
            String interactionId,
            String userId,
            String productId,
            InteractionType type,
            BigDecimal weight,
            Instant occurredAt,
            String sourceEventId) {
        this.interactionId = required(interactionId, "interactionId");
        this.userId = required(userId, "userId");
        this.productId = required(productId, "productId");
        this.type = requireType(type);
        this.weight = requireWeight(weight);
        this.occurredAt = requireOccurredAt(occurredAt);
        this.sourceEventId = sourceEventId == null || sourceEventId.isBlank()
                ? null
                : sourceEventId.trim();
    }

    public static UserInteraction create(
            String interactionId,
            String userId,
            String productId,
            InteractionType type,
            BigDecimal weight,
            Instant occurredAt) {
        return new UserInteraction(interactionId, userId, productId, type, weight, occurredAt);
    }

    public static UserInteraction create(
            String interactionId,
            String userId,
            String productId,
            InteractionType type,
            BigDecimal weight,
            Instant occurredAt,
            String sourceEventId) {
        return new UserInteraction(
                interactionId, userId, productId, type, weight, occurredAt, sourceEventId);
    }

    /** Whether a retry of the producing event can be recognised and dropped. */
    public boolean hasSourceEventId() {
        return sourceEventId != null;
    }

    public boolean isStrongSignal() {
        return type.isStrongSignal();
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new RecommendationException(RecommendationErrorCode.INTERACTION_INVALID,
                    field + " must not be blank");
        }
        return value.trim();
    }

    private static InteractionType requireType(InteractionType type) {
        if (type == null) {
            throw new RecommendationException(RecommendationErrorCode.INTERACTION_TYPE_UNSUPPORTED,
                    "interaction type must not be null");
        }
        return type;
    }

    private static BigDecimal requireWeight(BigDecimal weight) {
        if (weight == null || weight.signum() <= 0) {
            throw new RecommendationException(RecommendationErrorCode.INTERACTION_INVALID,
                    "weight must be positive");
        }
        return weight;
    }

    private static Instant requireOccurredAt(Instant occurredAt) {
        if (occurredAt == null) {
            throw new RecommendationException(RecommendationErrorCode.INTERACTION_INVALID,
                    "occurredAt must not be null");
        }
        return occurredAt;
    }
}
