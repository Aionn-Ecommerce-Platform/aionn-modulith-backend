package com.aionn.identity.application.dto.kyc.result;

import java.time.Instant;

public record KycResult(
        String kycId,
        String userId,
        String docType,
        String blobUrl,
        String status,
        String provider,
        String providerApplicantId,
        String providerLevelName,
        String providerReviewStatus,
        String reviewerId,
        String reviewNote,
        String decisionAdminId,
        String rejectReason,
        Instant submittedAt,
        Instant approvedAt) {
}
