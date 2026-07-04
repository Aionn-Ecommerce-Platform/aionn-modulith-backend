package com.aionn.identity.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_profiles", indexes = {
        @Index(name = "idx_kyc_user_id", columnList = "user_id"),
        @Index(name = "idx_kyc_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class KycProfileEntity {

    @Id
    @Column(name = "kyc_id", nullable = false, length = 26)
    private String kycId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "doc_type", nullable = false, length = 50)
    private String docType;

    @Column(name = "blob_url", columnDefinition = "TEXT")
    private String blobUrl;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "provider", length = 30)
    private String provider;

    @Column(name = "provider_applicant_id", length = 64)
    private String providerApplicantId;

    @Column(name = "provider_level_name", length = 128)
    private String providerLevelName;

    @Column(name = "provider_review_status", length = 50)
    private String providerReviewStatus;

    @Column(name = "provider_correlation_id", length = 128)
    private String providerCorrelationId;

    @Column(name = "reviewer_id", length = 26)
    private String reviewerId;

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;

    @Column(name = "decision_admin_id", length = 26)
    private String decisionAdminId;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
