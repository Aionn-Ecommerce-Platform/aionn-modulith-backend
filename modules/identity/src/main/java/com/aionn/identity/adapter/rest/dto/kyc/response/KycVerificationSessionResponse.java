package com.aionn.identity.adapter.rest.dto.kyc.response;

public record KycVerificationSessionResponse(
        String kycId,
        String provider,
        String providerApplicantId,
        String levelName,
        String sdkAccessToken,
        int expiresInSeconds,
        boolean sandbox) {
}
