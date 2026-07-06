package com.aionn.catalog.domain.model;

import com.aionn.catalog.domain.event.ReviewEvents;
import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import com.aionn.catalog.domain.valueobject.ReviewStatus;
import com.aionn.sharedkernel.domain.model.AggregateRoot;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
public class ProductReview extends AggregateRoot {

    private final String reviewId;
    private final String productId;
    private final String userId;
    private final String orderId;
    private int rating;
    private String title;
    private String content;
    private final List<String> imageUrls = new ArrayList<>();
    private ReviewStatus status;
    private String merchantReply;
    private Instant merchantRepliedAt;
    private String reportedByMerchantId;
    private String reportReason;
    private Instant reportedAt;
    private final Instant createdAt;
    private Instant updatedAt;

    public ProductReview(
            String reviewId,
            String productId,
            String userId,
            String orderId,
            int rating,
            String title,
            String content,
            List<String> imageUrls,
            ReviewStatus status,
            String merchantReply,
            Instant merchantRepliedAt,
            String reportedByMerchantId,
            String reportReason,
            Instant reportedAt,
            Instant createdAt,
            Instant updatedAt) {
        this.reviewId = reviewId;
        this.productId = productId;
        this.userId = userId;
        this.orderId = orderId;
        validateRating(rating);
        this.rating = rating;
        this.title = title;
        this.content = content;
        if (imageUrls != null) {
            validateImages(imageUrls);
            this.imageUrls.addAll(imageUrls);
        }
        this.status = status != null ? status : ReviewStatus.VISIBLE;
        this.merchantReply = merchantReply;
        this.merchantRepliedAt = merchantRepliedAt;
        this.reportedByMerchantId = reportedByMerchantId;
        this.reportReason = reportReason;
        this.reportedAt = reportedAt;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static ProductReview create(
            String reviewId,
            String productId,
            String userId,
            String orderId,
            int rating,
            String title,
            String content,
            List<String> imageUrls) {
        ProductReview review = new ProductReview(
                reviewId, productId, userId, orderId, rating, title, content, imageUrls,
                ReviewStatus.VISIBLE, null, null, null, null, null, Instant.now(), Instant.now());
        review.registerEvent(new ReviewEvents.ReviewCreated(reviewId, productId, userId, rating));
        return review;
    }

    public void update(int newRating, String newTitle, String newContent, List<String> newImageUrls) {
        if (this.status == ReviewStatus.HIDDEN || this.status == ReviewStatus.DELETED) {
            throw new CatalogException(CatalogErrorCode.REVIEW_FORBIDDEN, "Cannot update a hidden or deleted review");
        }
        validateRating(newRating);
        if (newImageUrls != null) {
            validateImages(newImageUrls);
        }
        this.rating = newRating;
        this.title = newTitle;
        this.content = newContent;
        this.imageUrls.clear();
        if (newImageUrls != null) {
            this.imageUrls.addAll(newImageUrls);
        }
        this.updatedAt = Instant.now();
        registerEvent(new ReviewEvents.ReviewUpdated(reviewId, rating));
    }

    public void reply(String replyContent) {
        if (this.status == ReviewStatus.HIDDEN || this.status == ReviewStatus.DELETED) {
            throw new CatalogException(CatalogErrorCode.REVIEW_FORBIDDEN, "Cannot reply to a hidden or deleted review");
        }
        this.merchantReply = replyContent;
        this.merchantRepliedAt = Instant.now();
        this.updatedAt = Instant.now();
        registerEvent(new ReviewEvents.MerchantReplied(reviewId));
    }

    public void hide() {
        this.status = ReviewStatus.HIDDEN;
        this.updatedAt = Instant.now();
        registerEvent(new ReviewEvents.ReviewHidden(reviewId));
    }

    public void report(String merchantId, String reason) {
        if (this.status == ReviewStatus.REPORTED) {
            throw new CatalogException(CatalogErrorCode.REVIEW_ALREADY_REPORTED);
        }
        if (this.status == ReviewStatus.DELETED) {
            throw new CatalogException(CatalogErrorCode.REVIEW_FORBIDDEN, "Cannot report a deleted review");
        }
        this.status = ReviewStatus.REPORTED;
        this.reportedByMerchantId = merchantId;
        this.reportReason = reason;
        this.reportedAt = Instant.now();
        this.updatedAt = Instant.now();
        registerEvent(new ReviewEvents.ReviewReported(reviewId, merchantId, reason));
    }

    public void adminDelete(String adminId) {
        this.status = ReviewStatus.DELETED;
        this.updatedAt = Instant.now();
        registerEvent(new ReviewEvents.ReviewDeleted(reviewId, adminId));
    }

    public void restore(String adminId) {
        if (this.status != ReviewStatus.REPORTED) {
            throw new CatalogException(CatalogErrorCode.REVIEW_NOT_REPORTED);
        }
        this.status = ReviewStatus.VISIBLE;
        this.reportedByMerchantId = null;
        this.reportReason = null;
        this.reportedAt = null;
        this.updatedAt = Instant.now();
        registerEvent(new ReviewEvents.ReviewRestored(reviewId, adminId));
    }

    @Override
    protected String aggregateId() {
        return reviewId;
    }

    private void validateRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new CatalogException(CatalogErrorCode.REVIEW_INVALID_RATING);
        }
    }

    private void validateImages(List<String> images) {
        if (images.size() > 5) {
            throw new CatalogException(CatalogErrorCode.INVALID_ARGUMENT, "A review cannot have more than 5 images");
        }
    }
}
