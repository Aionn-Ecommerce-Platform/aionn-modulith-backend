package com.aionn.recommendation.domain.model;

import com.aionn.recommendation.domain.exception.RecommendationErrorCode;
import com.aionn.recommendation.domain.exception.RecommendationException;
import com.aionn.recommendation.domain.valueobject.InteractionType;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** One recorded behavioural signal. Immutable: the log is append-only, later signals supersede and decay fades earlier ones. */
@Getter
public class UserInteraction {

    private final String interactionId;
    private final String userId;
    private final String productId;
    private final InteractionType type;
    private final BigDecimal weight;
    private final Instant occurredAt;

    /**
     * Integration event that produced this signal, or null for non-event writes (fixtures, backfills).
     * A unique index over it plus the natural key makes ingest idempotent under at-least-once delivery.
     */
    private final String sourceEventId;

    /** Facets carried by {@link InteractionType#SEARCH}: a search has no product to look up. */
    private final List<String> categoryIds;
    private final String brandId;
    private final String keyword;

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
        this(interactionId, userId, productId, type, weight, occurredAt, sourceEventId,
                List.of(), null, null);
    }

    public UserInteraction(
            String interactionId,
            String userId,
            String productId,
            InteractionType type,
            BigDecimal weight,
            Instant occurredAt,
            String sourceEventId,
            List<String> categoryIds,
            String brandId,
            String keyword) {
        this.interactionId = required(interactionId, "interactionId");
        this.userId = required(userId, "userId");
        this.type = requireType(type);
        // SEARCH records filter intent, not a product; every other type must resolve a productId.
        this.productId = type == InteractionType.SEARCH ? productId : required(productId, "productId");
        this.weight = requireWeight(weight);
        this.occurredAt = requireOccurredAt(occurredAt);
        this.sourceEventId = sourceEventId == null || sourceEventId.isBlank()
                ? null
                : sourceEventId.trim();
        this.categoryIds = categoryIds == null ? List.of() : List.copyOf(categoryIds);
        this.brandId = brandId;
        this.keyword = keyword;
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

    public static UserInteraction createSearch(
            String interactionId,
            String userId,
            InteractionType type,
            BigDecimal weight,
            Instant occurredAt,
            String sourceEventId,
            List<String> categoryIds,
            String brandId,
            String keyword) {
        return new UserInteraction(
                interactionId, userId, null, type, weight, occurredAt, sourceEventId,
                categoryIds, brandId, keyword);
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
