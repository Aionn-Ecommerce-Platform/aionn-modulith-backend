package com.aionn.identity.domain.model;

import com.aionn.identity.domain.valueobject.KycReviewAnswer;
import com.aionn.identity.domain.valueobject.KycStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Clock;
import java.time.Instant;

@Getter
@AllArgsConstructor
public class KycProfile {
    private final String kycId;
    private final String userId;
    private final String docType;
    private String blobUrl;
    private KycStatus status;
    private String provider;
    private String providerApplicantId;
    private String providerLevelName;
    private String providerReviewStatus;
    private String providerCorrelationId;
    private String reviewerId;
    private String reviewNote;
    private String decisionAdminId;
    private String rejectReason;
    private Instant submittedAt;
    private Instant approvedAt;
    private final Instant createdAt;

    public void attachExternalProvider(
            String provider,
            String providerApplicantId,
            String providerLevelName,
            String providerReviewStatus,
            String providerCorrelationId) {
        attachExternalProvider(provider, providerApplicantId, providerLevelName,
                providerReviewStatus, providerCorrelationId, Clock.systemUTC());
    }

    public void attachExternalProvider(
            String provider,
            String providerApplicantId,
            String providerLevelName,
            String providerReviewStatus,
            String providerCorrelationId,
            Clock clock) {
        this.provider = provider;
        this.providerApplicantId = providerApplicantId;
        this.providerLevelName = providerLevelName;
        this.providerReviewStatus = providerReviewStatus;
        this.providerCorrelationId = providerCorrelationId;
        transitionTo(KycStatus.SUBMITTED);
        if (this.submittedAt == null) {
            this.submittedAt = clock.instant();
        }
    }

    public void syncExternalReview(
            String providerReviewStatus,
            String providerCorrelationId,
            KycReviewAnswer reviewAnswer,
            String moderationComment,
            String clientComment) {
        syncExternalReview(providerReviewStatus, providerCorrelationId, reviewAnswer,
                moderationComment, clientComment, Clock.systemUTC());
    }

    public void syncExternalReview(
            String providerReviewStatus,
            String providerCorrelationId,
            KycReviewAnswer reviewAnswer,
            String moderationComment,
            String clientComment,
            Clock clock) {
        this.providerReviewStatus = providerReviewStatus;
        this.providerCorrelationId = providerCorrelationId;
        this.reviewNote = moderationComment;

        if (reviewAnswer == null) {
            if (this.status == KycStatus.DRAFT) {
                transitionTo(KycStatus.SUBMITTED);
                this.submittedAt = this.submittedAt == null ? clock.instant() : this.submittedAt;
            }
            return;
        }

        switch (reviewAnswer) {
            case GREEN -> {
                transitionTo(KycStatus.APPROVED);
                this.decisionAdminId = providerDecisionSource();
                this.rejectReason = null;
                this.approvedAt = clock.instant();
            }
            case RED -> {
                transitionTo(KycStatus.REJECTED);
                this.decisionAdminId = providerDecisionSource();
                this.rejectReason = moderationComment != null && !moderationComment.isBlank()
                        ? moderationComment
                        : clientComment;
                this.approvedAt = null;
            }
        }
    }

    public boolean isManagedExternally() {
        return provider != null && !provider.isBlank();
    }

    public void attachBlobUrlIfEmpty(String url) {
        if ((this.blobUrl == null || this.blobUrl.isBlank()) && url != null && !url.isBlank()) {
            this.blobUrl = url;
        }
    }

    public void submit() {
        submit(Clock.systemUTC());
    }

    public void submit(Clock clock) {
        transitionTo(KycStatus.SUBMITTED);
        this.submittedAt = clock.instant();
        this.reviewerId = null;
        this.reviewNote = null;
        this.decisionAdminId = null;
        this.rejectReason = null;
        this.approvedAt = null;
    }

    public void adminApprove(String adminId, String note) {
        adminApprove(adminId, note, Clock.systemUTC());
    }

    public void adminApprove(String adminId, String note, Clock clock) {
        transitionTo(KycStatus.APPROVED);
        this.decisionAdminId = adminId;
        this.reviewerId = adminId;
        this.reviewNote = note;
        this.rejectReason = null;
        this.approvedAt = clock.instant();
    }

    public void adminReject(String adminId, String reason) {
        transitionTo(KycStatus.REJECTED);
        this.decisionAdminId = adminId;
        this.reviewerId = adminId;
        this.rejectReason = reason;
        this.approvedAt = null;
    }

    public void adminMarkInReview(String adminId, String note) {
        transitionTo(KycStatus.IN_REVIEW);
        this.reviewerId = adminId;
        if (note != null && !note.isBlank()) {
            this.reviewNote = note;
        }
    }

    private String providerDecisionSource() {
        return provider == null || provider.isBlank()
                ? "SYSTEM"
                : provider.toUpperCase();
    }

    private void transitionTo(KycStatus newStatus) {
        if (this.status == newStatus) {
            return;
        }
        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    "KYC cannot transition from " + this.status + " to " + newStatus);
        }
        this.status = newStatus;
    }
}
