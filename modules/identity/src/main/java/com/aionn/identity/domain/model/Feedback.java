package com.aionn.identity.domain.model;

import com.aionn.identity.domain.valueobject.FeedbackCategory;
import com.aionn.identity.domain.valueobject.FeedbackStatus;

import java.time.LocalDateTime;

public class Feedback {

    private final String feedbackId;
    private final String userId;
    private final FeedbackCategory category;
    private final String subject;
    private final String content;
    private final Short rating;
    private final String contactEmail;
    private final String contactPhone;
    private FeedbackStatus status;
    private String handledBy;
    private LocalDateTime handledAt;
    private String adminReply;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Feedback(
            String feedbackId,
            String userId,
            FeedbackCategory category,
            String subject,
            String content,
            Short rating,
            String contactEmail,
            String contactPhone,
            FeedbackStatus status,
            String handledBy,
            LocalDateTime handledAt,
            String adminReply,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.feedbackId = feedbackId;
        this.userId = userId;
        this.category = category;
        this.subject = subject;
        this.content = content;
        this.rating = rating;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.status = status;
        this.handledBy = handledBy;
        this.handledAt = handledAt;
        this.adminReply = adminReply;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Feedback createNew(
            String feedbackId,
            String userId,
            FeedbackCategory category,
            String subject,
            String content,
            Integer rating,
            String contactEmail,
            String contactPhone) {
        return new Feedback(
                feedbackId,
                userId,
                category,
                subject,
                content,
                rating == null ? null : rating.shortValue(),
                contactEmail,
                contactPhone,
                FeedbackStatus.OPEN,
                null,
                null,
                null,
                null,
                null);
    }

    public void reply(String adminId, String reply, FeedbackStatus newStatus, LocalDateTime handledAt) {
        this.adminReply = reply;
        this.handledBy = adminId;
        this.handledAt = handledAt;
        if (newStatus != null) {
            this.status = newStatus;
        } else if (this.status == FeedbackStatus.OPEN) {
            this.status = FeedbackStatus.IN_REVIEW;
        }
    }

    public void changeStatus(String adminId, FeedbackStatus newStatus, LocalDateTime handledAt) {
        this.status = newStatus;
        this.handledBy = adminId;
        this.handledAt = handledAt;
    }

    public String getFeedbackId() {
        return feedbackId;
    }

    public String getUserId() {
        return userId;
    }

    public FeedbackCategory getCategory() {
        return category;
    }

    public String getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
    }

    public Short getRating() {
        return rating;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public FeedbackStatus getStatus() {
        return status;
    }

    public String getHandledBy() {
        return handledBy;
    }

    public LocalDateTime getHandledAt() {
        return handledAt;
    }

    public String getAdminReply() {
        return adminReply;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
