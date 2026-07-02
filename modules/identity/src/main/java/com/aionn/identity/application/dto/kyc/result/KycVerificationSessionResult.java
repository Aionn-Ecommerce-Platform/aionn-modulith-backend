package com.aionn.identity.application.dto.kyc.result;

public record KycVerificationSessionResult(
        String kycId,
        String provider,
        String providerApplicantId,
        String levelName,
        String sdkAccessToken,
        int expiresInSeconds,
        boolean sandbox) {
    @Override
    public String toString() {
        return "KycVerificationSessionResult[kycId=%s, provider=%s, providerApplicantId=%s, levelName=%s, sdkAccessToken=***, expiresInSeconds=%s, sandbox=%s]"
                .formatted(kycId, provider, providerApplicantId, levelName, expiresInSeconds, sandbox);
    }
}
