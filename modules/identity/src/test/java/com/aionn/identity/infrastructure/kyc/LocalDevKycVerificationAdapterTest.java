package com.aionn.identity.infrastructure.kyc;

import com.aionn.identity.application.port.out.kyc.ExternalKycVerificationPort.ExternalKycApplicant;
import com.aionn.identity.application.port.out.kyc.ExternalKycVerificationPort.ExternalKycSession;
import com.aionn.identity.domain.model.IdentityUser;
import com.aionn.identity.domain.valueobject.KycProvider;
import com.aionn.identity.infrastructure.config.properties.KycProperties;
import com.aionn.identity.infrastructure.config.properties.KycProperties.Local;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class LocalDevKycVerificationAdapterTest {

    private final IdentityUser user = IdentityUser.createNew("user-1", "user@example.com", "+8490", "john");

    private LocalDevKycVerificationAdapter adapter;

    @BeforeEach
    void setUp() {
        KycProperties properties = new KycProperties(
                KycProvider.LOCAL, null, new Local("my-level", Duration.ofMinutes(5)));
        adapter = new LocalDevKycVerificationAdapter(properties);
    }

    @Test
    void createApplicantReturnsCannedLocalApplicant() {
        ExternalKycApplicant applicant = adapter.createApplicant(user, "kyc-9", "PASSPORT");

        assertThat(applicant.provider()).isEqualTo("local");
        assertThat(applicant.applicantId()).isEqualTo("local-applicant-kyc-9");
        assertThat(applicant.levelName()).isEqualTo("my-level");
        assertThat(applicant.reviewStatus()).isEqualTo("completed");
        assertThat(applicant.correlationId()).isEqualTo("local-correlation-kyc-9");
    }

    @Test
    void generateVerificationSessionReturnsCannedLocalSession() {
        ExternalKycSession session = adapter.generateVerificationSession(user, "kyc-9", "applicant-1");

        assertThat(session.provider()).isEqualTo("local");
        assertThat(session.applicantId()).isEqualTo("applicant-1");
        assertThat(session.levelName()).isEqualTo("my-level");
        assertThat(session.accessToken()).isEqualTo("local-sdk-token-kyc-9");
        assertThat(session.expiresInSeconds()).isEqualTo(300);
        assertThat(session.sandbox()).isTrue();
    }

    @Test
    void verifyWebhookSignatureIsNoOp() {
        assertThatCode(() -> adapter.verifyWebhookSignature(new byte[] { 1, 2 }, "digest", "sha256"))
                .doesNotThrowAnyException();
    }
}
