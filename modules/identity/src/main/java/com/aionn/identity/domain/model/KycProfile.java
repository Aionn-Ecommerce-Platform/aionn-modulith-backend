package com.aionn.identity.domain.model;

import com.aionn.identity.domain.valueobject.KycReviewAnswer;
import com.aionn.identity.domain.valueobject.KycStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

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
    private LocalDateTime submittedAt;
    private LocalDateTime approvedAt;
    private final LocalDateTime createdAt;

    public void attachExternalProvider(
            String provider,
            String providerApplicantId,
            String providerLevelName,
            String providerReviewStatus,
            String providerCorrelationId) {
        this.provider = provider;
        this.providerApplicantId = providerApplicantId;
        this.providerLevelName = providerLevelName;
        this.providerReviewStatus = providerReviewStatus;
        this.providerCorrelationId = providerCorrelationId;
        this.status = KycStatus.SUBMITTED;
        if (this.submittedAt == null) {
            this.submittedAt = LocalDateTime.now();
        }
    }

    public void syncExternalReview(
            String providerReviewStatus,
            String providerCorrelationId,
            KycReviewAnswer reviewAnswer,
            String moderationComment,
            String clientComment) {
        this.providerReviewStatus = providerReviewStatus;
        this.providerCorrelationId = providerCorrelationId;
        this.reviewNote = moderationComment;

        if (reviewAnswer == null) {
            if (this.status == KycStatus.DRAFT) {
                this.status = KycStatus.SUBMITTED;
                this.submittedAt = this.submittedAt == null ? LocalDateTime.now() : this.submittedAt;
            }
            return;
        }

        switch (reviewAnswer) {
            case GREEN -> {
                this.status = KycStatus.APPROVED;
                this.decisionAdminId = providerDecisionSource();
                this.rejectReason = null;
                this.approvedAt = LocalDateTime.now();
            }
            case RED -> {
                this.status = KycStatus.REJECTED;
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
        if (this.status != KycStatus.DRAFT && this.status != KycStatus.REJECTED) {
            throw new IllegalStateException(
                    "KYC can only be submitted from DRAFT or REJECTED (current=" + this.status + ")");
        }
        this.status = KycStatus.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
        this.reviewerId = null;
        this.reviewNote = null;
        this.decisionAdminId = null;
        this.rejectReason = null;
        this.approvedAt = null;
    }

    public void adminApprove(String adminId, String note) {
        if (this.status != KycStatus.SUBMITTED && this.status != KycStatus.IN_REVIEW) {
            throw new IllegalStateException(
                    "KYC can only be approved from SUBMITTED or IN_REVIEW (current=" + this.status + ")");
        }
        this.status = KycStatus.APPROVED;
        this.decisionAdminId = adminId;
        this.reviewerId = adminId;
        this.reviewNote = note;
        this.rejectReason = null;
        this.approvedAt = LocalDateTime.now();
    }

    public void adminReject(String adminId, String reason) {
        if (this.status != KycStatus.SUBMITTED && this.status != KycStatus.IN_REVIEW) {
            throw new IllegalStateException(
                    "KYC can only be rejected from SUBMITTED or IN_REVIEW (current=" + this.status + ")");
        }
        this.status = KycStatus.REJECTED;
        this.decisionAdminId = adminId;
        this.reviewerId = adminId;
        this.rejectReason = reason;
        this.approvedAt = null;
    }

    public void adminMarkInReview(String adminId, String note) {
        if (this.status != KycStatus.SUBMITTED) {
            throw new IllegalStateException(
                    "KYC can only enter IN_REVIEW from SUBMITTED (current=" + this.status + ")");
        }
        this.status = KycStatus.IN_REVIEW;
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
}
