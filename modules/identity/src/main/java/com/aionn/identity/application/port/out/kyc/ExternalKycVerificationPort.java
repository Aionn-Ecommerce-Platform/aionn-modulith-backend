package com.aionn.identity.application.port.out.kyc;

import com.aionn.identity.domain.model.IdentityUser;

public interface ExternalKycVerificationPort {

    ExternalKycApplicant createApplicant(IdentityUser user, String kycId, String docType);

    ExternalKycSession generateVerificationSession(IdentityUser user, String kycId, String providerApplicantId);

    /**
     * Verifies the webhook signature and throws {@link SecurityException} when
     * the signature is missing, malformed, or does not match the payload.
     */
    void verifyWebhookSignature(byte[] payload, String digest, String digestAlgorithm);

    record ExternalKycApplicant(
            String provider,
            String applicantId,
            String levelName,
            String reviewStatus,
            String correlationId) {
    }

    record ExternalKycSession(
            String provider,
            String applicantId,
            String levelName,
            String accessToken,
            int expiresInSeconds,
            boolean sandbox) {
    }
}
