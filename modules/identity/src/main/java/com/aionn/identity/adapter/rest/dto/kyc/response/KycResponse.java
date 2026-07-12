package com.aionn.identity.adapter.rest.dto.kyc.response;

import java.time.Instant;

public record KycResponse(
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
