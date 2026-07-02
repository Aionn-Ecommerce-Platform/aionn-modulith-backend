package com.aionn.identity.application.dto.kyc.result;

public record KycVerificationSessionResult(
        String kycId,
        String provider,
        String providerApplicantId,
        String levelName,
        String sdkAccessToken,
        int expiresInSeconds,
        boolean sandbox) {
}
