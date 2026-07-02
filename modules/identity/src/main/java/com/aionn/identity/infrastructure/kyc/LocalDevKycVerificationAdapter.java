package com.aionn.identity.infrastructure.kyc;

import com.aionn.identity.application.port.out.kyc.ExternalKycVerificationPort;
import com.aionn.identity.domain.model.IdentityUser;
import com.aionn.identity.infrastructure.config.properties.KycProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "identity.kyc", name = "provider", havingValue = "local")
public class LocalDevKycVerificationAdapter implements ExternalKycVerificationPort {

    private static final String PROVIDER_CODE = "local";

    private final KycProperties kycProperties;

    @Override
    public ExternalKycApplicant createApplicant(IdentityUser user, String kycId, String docType) {
        String applicantId = "local-applicant-" + kycId;
        String correlationId = "local-correlation-" + kycId;
        log.info("Local dev KYC applicant created for user={}, kycId={}", user.getUserId(), kycId);
        return new ExternalKycApplicant(
                PROVIDER_CODE,
                applicantId,
                kycProperties.local().levelName(),
                "completed",
                correlationId);
    }

    @Override
    public ExternalKycSession generateVerificationSession(IdentityUser user, String kycId, String providerApplicantId) {
        String accessToken = "local-sdk-token-" + kycId;
        log.info("Local dev KYC session generated for user={}, kycId={}", user.getUserId(), kycId);
        return new ExternalKycSession(
                PROVIDER_CODE,
                providerApplicantId,
                kycProperties.local().levelName(),
                accessToken,
                kycProperties.local().sessionTtlSeconds(),
                true);
    }

    @Override
    public void verifyWebhookSignature(byte[] payload, String digest, String digestAlgorithm) {
    }
}
