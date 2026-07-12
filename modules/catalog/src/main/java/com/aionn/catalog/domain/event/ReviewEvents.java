package com.aionn.catalog.domain.event;

import java.time.Clock;
import java.time.Instant;

public final class ReviewEvents {

    private ReviewEvents() {
    }

    public record ReviewCreated(
            String reviewId,
            String productId,
            String userId,
            int rating,
            Instant occurredAt) implements CatalogEvent {
        public ReviewCreated(String reviewId, String productId, String userId, int rating) {
            this(reviewId, productId, userId, rating, Clock.systemUTC());
        }
        public ReviewCreated(String reviewId, String productId, String userId, int rating, Clock clock) {
            this(reviewId, productId, userId, rating, clock.instant());
        }
    }

    public record ReviewUpdated(
            String reviewId,
            int rating,
            Instant occurredAt) implements CatalogEvent {
        public ReviewUpdated(String reviewId, int rating) {
            this(reviewId, rating, Clock.systemUTC());
        }
        public ReviewUpdated(String reviewId, int rating, Clock clock) {
            this(reviewId, rating, clock.instant());
        }
    }

    public record ReviewHidden(
            String reviewId,
            Instant occurredAt) implements CatalogEvent {
        public ReviewHidden(String reviewId) {
            this(reviewId, Clock.systemUTC());
        }
        public ReviewHidden(String reviewId, Clock clock) {
            this(reviewId, clock.instant());
        }
    }

    public record ReviewReported(
            String reviewId,
            String reportedByMerchantId,
            String reason,
            Instant occurredAt) implements CatalogEvent {
        public ReviewReported(String reviewId, String reportedByMerchantId, String reason) {
            this(reviewId, reportedByMerchantId, reason, Clock.systemUTC());
        }
        public ReviewReported(String reviewId, String reportedByMerchantId, String reason, Clock clock) {
            this(reviewId, reportedByMerchantId, reason, clock.instant());
        }
    }

    public record ReviewDeleted(
            String reviewId,
            String deletedByAdminId,
            Instant occurredAt) implements CatalogEvent {
        public ReviewDeleted(String reviewId, String deletedByAdminId) {
            this(reviewId, deletedByAdminId, Clock.systemUTC());
        }
        public ReviewDeleted(String reviewId, String deletedByAdminId, Clock clock) {
            this(reviewId, deletedByAdminId, clock.instant());
        }
    }

    public record ReviewRestored(
            String reviewId,
            String restoredByAdminId,
            Instant occurredAt) implements CatalogEvent {
        public ReviewRestored(String reviewId, String restoredByAdminId) {
            this(reviewId, restoredByAdminId, Clock.systemUTC());
        }
        public ReviewRestored(String reviewId, String restoredByAdminId, Clock clock) {
            this(reviewId, restoredByAdminId, clock.instant());
        }
    }

    public record MerchantReplied(
            String reviewId,
            Instant occurredAt) implements CatalogEvent {
        public MerchantReplied(String reviewId) {
            this(reviewId, Clock.systemUTC());
        }
        public MerchantReplied(String reviewId, Clock clock) {
            this(reviewId, clock.instant());
        }
    }
}
