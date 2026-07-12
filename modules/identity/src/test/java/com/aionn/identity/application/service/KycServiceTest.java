package com.aionn.identity.application.service;

import com.aionn.identity.application.dto.kyc.command.SumsubWebhookCommand;
import com.aionn.identity.application.dto.kyc.result.KycVerificationSessionResult;
import com.aionn.identity.application.policy.KycPolicy;
import com.aionn.identity.application.port.out.kyc.ExternalKycVerificationPort;
import com.aionn.identity.application.port.out.kyc.KycPersistencePort;
import com.aionn.identity.application.port.out.user.UserPersistencePort;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.model.IdentityUser;
import com.aionn.identity.domain.model.KycDocument;
import com.aionn.identity.domain.model.KycProfile;
import com.aionn.identity.domain.valueobject.KycDocumentStatus;
import com.aionn.identity.domain.valueobject.KycDocumentType;
import com.aionn.identity.domain.valueobject.KycStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class KycServiceTest {

    private static final String USER_ID = "user-1";
    private static final String KYC_ID = "kyc-1";

    @Mock
    private KycPersistencePort kycPersistencePort;
    @Mock
    private UserPersistencePort userPersistencePort;
    @Mock
    private KycPolicy kycPolicy;
    @Mock
    private ExternalKycVerificationPort externalKycVerificationPort;

    private KycService kycService;

    @BeforeEach
    void setUp() {
        kycService = new KycService(
                kycPersistencePort, userPersistencePort, kycPolicy, externalKycVerificationPort,
Clock.systemUTC());
    }

    private static IdentityUser activeUser() {
        return IdentityUser.createNew(USER_ID, "u@example.com", null, "user");
    }

    private static KycProfile draftProfile() {
        return new KycProfile(
                KYC_ID, USER_ID, "ID_CARD",
                null, KycStatus.DRAFT,
                null, null, null, null, null,
                null, null, null, null,
                null, null, Instant.now());
    }

    private static KycProfile submittedProfile() {
        KycProfile k = draftProfile();
        k.attachExternalProvider("SUMSUB", "applicant-1", "basic", "init", "corr-1");
        return k;
    }

    @Test
    void planKycCreationNormalizesDocumentType() {
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(kycPolicy.usesManagedProvider()).thenReturn(false);

        KycService.KycCreationPlan plan = kycService.planKycCreation(USER_ID, "cccd");

        assertThat(plan.user().getUserId()).isEqualTo(USER_ID);
        assertThat(plan.docType()).isEqualTo("CCCD");
        assertThat(plan.managedProviderEnabled()).isFalse();
    }

    @Test
    void createKycInDraftWhenManagedProviderDisabled() {
        KycService.KycCreationPlan plan = new KycService.KycCreationPlan(
                KYC_ID,
                activeUser(),
                "ID_CARD",
                false);
        when(kycPersistencePort.save(any(KycProfile.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        KycProfile created = kycService.createKyc(plan, null);

        assertThat(created.getStatus()).isEqualTo(KycStatus.DRAFT);
        verify(externalKycVerificationPort, never()).createApplicant(any(), any(), any());
    }

    @Test
    void createKycRejectsUnknownDocumentType() {
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));

        IdentityException ex = assertThrows(IdentityException.class,
                () -> kycService.planKycCreation(USER_ID, "DRIVER_LICENSE"));

        assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.KYC_DOCUMENT_TYPE_INVALID.getCode());
        verify(kycPersistencePort, never()).save(any());
    }

    @Test
    void createKycAttachesExternalProviderWhenManaged() {
        KycService.KycCreationPlan plan = new KycService.KycCreationPlan(
                KYC_ID,
                activeUser(),
                "ID_CARD",
                true);
        var applicant = new ExternalKycVerificationPort.ExternalKycApplicant(
                "SUMSUB", "applicant-9", "basic", "init", "corr-1");
        when(kycPersistencePort.save(any(KycProfile.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        KycProfile created = kycService.createKyc(plan, applicant);

        assertThat(created.getStatus()).isEqualTo(KycStatus.SUBMITTED);
        assertThat(created.getProvider()).isEqualTo("SUMSUB");
        assertThat(created.getProviderApplicantId()).isEqualTo("applicant-9");
    }

    @Test
    void attachDocumentStoresDocumentAndLegacyBlobUrl() {
        KycProfile profile = draftProfile();
        KycDocument savedDocument = new KycDocument(
                "doc-1",
                KYC_ID,
                KycDocumentType.ID_FRONT,
                "https://cdn.test/front.jpg",
                "identity/kyc/front",
                KycDocumentStatus.UPLOADED,
                Instant.now());
        when(kycPersistencePort.findByKycIdAndUserId(KYC_ID, USER_ID)).thenReturn(Optional.of(profile));
        when(kycPersistencePort.saveDocument(any(KycDocument.class))).thenReturn(savedDocument);
        when(kycPersistencePort.save(profile)).thenReturn(profile);

        KycDocument result = kycService.attachDocument(
                USER_ID,
                KYC_ID,
                "ID_FRONT",
                "https://cdn.test/front.jpg",
                "identity/kyc/front");

        assertThat(result).isSameAs(savedDocument);
        assertThat(profile.getBlobUrl()).isEqualTo("https://cdn.test/front.jpg");
        verify(kycPersistencePort).save(profile);
    }

    @Test
    void submitRequiresFrontAndBackForIdCard() {
        KycProfile profile = draftProfile();
        when(kycPersistencePort.findByKycIdAndUserId(KYC_ID, USER_ID)).thenReturn(Optional.of(profile));
        when(kycPersistencePort.findDocumentsByKycId(KYC_ID)).thenReturn(List.of(
                new KycDocument("doc-front", KYC_ID, KycDocumentType.ID_FRONT,
                        "https://cdn.test/front.jpg", null, KycDocumentStatus.UPLOADED, Instant.now())));

        assertThrows(IdentityException.class, () -> kycService.submit(USER_ID, KYC_ID));
    }

    @Test
    void submitMovesLocalProfileToSubmittedWhenRequiredDocumentsExist() {
        KycProfile profile = draftProfile();
        when(kycPersistencePort.findByKycIdAndUserId(KYC_ID, USER_ID)).thenReturn(Optional.of(profile));
        when(kycPersistencePort.findDocumentsByKycId(KYC_ID)).thenReturn(List.of(
                new KycDocument("doc-front", KYC_ID, KycDocumentType.ID_FRONT,
                        "https://cdn.test/front.jpg", null, KycDocumentStatus.UPLOADED, Instant.now()),
                new KycDocument("doc-back", KYC_ID, KycDocumentType.ID_BACK,
                        "https://cdn.test/back.jpg", null, KycDocumentStatus.UPLOADED, Instant.now())));
        when(kycPersistencePort.save(profile)).thenReturn(profile);

        KycProfile result = kycService.submit(USER_ID, KYC_ID);

        assertThat(result).isSameAs(profile);
        assertThat(result.getStatus()).isEqualTo(KycStatus.SUBMITTED);
        assertThat(result.getSubmittedAt()).isNotNull();
    }

    @Test
    void adminApproveTransitionsSubmittedToApproved() {
        KycProfile profile = submittedProfile();
        when(kycPersistencePort.findById(KYC_ID)).thenReturn(Optional.of(profile));
        when(kycPersistencePort.save(profile)).thenReturn(profile);

        KycProfile result = kycService.adminApprove(KYC_ID, "admin-1", "ok");

        assertThat(result.getStatus()).isEqualTo(KycStatus.APPROVED);
        assertThat(result.getDecisionAdminId()).isEqualTo("admin-1");
        assertThat(result.getApprovedAt()).isNotNull();
    }

    @Test
    void adminApproveOnUnknownProfileThrows() {
        when(kycPersistencePort.findById(KYC_ID)).thenReturn(Optional.empty());

        assertThrows(IdentityException.class,
                () -> kycService.adminApprove(KYC_ID, "admin", "n"));
    }

    @Test
    void listMyDelegatesToPersistencePort() {
        KycProfile p = submittedProfile();
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(kycPersistencePort.findByUserIdOrderBySubmittedAtDesc(USER_ID))
                .thenReturn(List.of(p));

        List<KycProfile> result = kycService.listMy(USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isSameAs(p);
    }

    @Test
    void adminRejectFromDraftRaisesInvalidStateException() {
        KycProfile profile = draftProfile();
        when(kycPersistencePort.findById(KYC_ID)).thenReturn(Optional.of(profile));

        assertThrows(IdentityException.class,
                () -> kycService.adminReject(KYC_ID, "admin", "bad"));
    }

    @Test
    void planKycCreationThrowsWhenUserMissing() {
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.empty());

        IdentityException ex = assertThrows(IdentityException.class,
                () -> kycService.planKycCreation(USER_ID, "ID_CARD"));

        assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.USER_NOT_FOUND.getCode());
    }

    @Test
    void createKycThrowsWhenManagedProviderApplicantMissing() {
        KycService.KycCreationPlan plan = new KycService.KycCreationPlan(
                KYC_ID, activeUser(), "ID_CARD", true);

        IdentityException ex = assertThrows(IdentityException.class,
                () -> kycService.createKyc(plan, null));

        assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.KYC_PROVIDER_ERROR.getCode());
        verify(kycPersistencePort, never()).save(any());
    }

    @Test
    void getReturnsProfileForOwner() {
        KycProfile profile = draftProfile();
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(kycPersistencePort.findByKycIdAndUserId(KYC_ID, USER_ID)).thenReturn(Optional.of(profile));

        assertThat(kycService.get(USER_ID, KYC_ID)).isSameAs(profile);
    }

    @Test
    void getThrowsWhenProfileMissing() {
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(kycPersistencePort.findByKycIdAndUserId(KYC_ID, USER_ID)).thenReturn(Optional.empty());

        assertThrows(IdentityException.class, () -> kycService.get(USER_ID, KYC_ID));
    }

    @Test
    void listMyThrowsWhenUserMissing() {
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(IdentityException.class, () -> kycService.listMy(USER_ID));
        verify(kycPersistencePort, never()).findByUserIdOrderBySubmittedAtDesc(any());
    }

    @Test
    void adminListByStatusDelegatesToPersistencePort() {
        KycProfile p = submittedProfile();
        when(kycPersistencePort.findByStatus(KycStatus.SUBMITTED, 25)).thenReturn(List.of(p));

        List<KycProfile> result = kycService.adminListByStatus(KycStatus.SUBMITTED, 25);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isSameAs(p);
    }

    @Test
    void adminGetReturnsProfile() {
        KycProfile p = submittedProfile();
        when(kycPersistencePort.findById(KYC_ID)).thenReturn(Optional.of(p));

        assertThat(kycService.adminGet(KYC_ID)).isSameAs(p);
    }

    @Test
    void adminGetThrowsWhenMissing() {
        when(kycPersistencePort.findById(KYC_ID)).thenReturn(Optional.empty());

        assertThrows(IdentityException.class, () -> kycService.adminGet(KYC_ID));
    }

    @Test
    void adminRejectTransitionsSubmittedToRejected() {
        KycProfile profile = submittedProfile();
        when(kycPersistencePort.findById(KYC_ID)).thenReturn(Optional.of(profile));
        when(kycPersistencePort.save(profile)).thenReturn(profile);

        KycProfile result = kycService.adminReject(KYC_ID, "admin-1", "blurry document");

        assertThat(result.getStatus()).isEqualTo(KycStatus.REJECTED);
        assertThat(result.getRejectReason()).isEqualTo("blurry document");
    }

    @Test
    void adminMarkInReviewTransitionsSubmittedToInReview() {
        KycProfile profile = submittedProfile();
        when(kycPersistencePort.findById(KYC_ID)).thenReturn(Optional.of(profile));
        when(kycPersistencePort.save(profile)).thenReturn(profile);

        KycProfile result = kycService.adminMarkInReview(KYC_ID, "admin-1", "manual check");

        assertThat(result.getStatus()).isEqualTo(KycStatus.IN_REVIEW);
        assertThat(result.getReviewerId()).isEqualTo("admin-1");
    }

    @Test
    void adminMarkInReviewOnUnknownProfileThrows() {
        when(kycPersistencePort.findById(KYC_ID)).thenReturn(Optional.empty());

        assertThrows(IdentityException.class,
                () -> kycService.adminMarkInReview(KYC_ID, "admin", "n"));
    }

    @Test
    void attachDocumentRejectsExternallyManagedProfile() {
        KycProfile profile = submittedProfile();
        when(kycPersistencePort.findByKycIdAndUserId(KYC_ID, USER_ID)).thenReturn(Optional.of(profile));

        IdentityException ex = assertThrows(IdentityException.class, () -> kycService.attachDocument(
                USER_ID, KYC_ID, "ID_FRONT", "https://cdn.test/front.jpg", null));

        assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.KYC_MANAGED_EXTERNALLY.getCode());
        verify(kycPersistencePort, never()).saveDocument(any());
    }

    @Test
    void attachDocumentRejectsWhenProfileAlreadySubmitted() {
        KycProfile profile = draftProfile();
        profile.submit();
        when(kycPersistencePort.findByKycIdAndUserId(KYC_ID, USER_ID)).thenReturn(Optional.of(profile));

        IdentityException ex = assertThrows(IdentityException.class, () -> kycService.attachDocument(
                USER_ID, KYC_ID, "ID_FRONT", "https://cdn.test/front.jpg", null));

        assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.KYC_INVALID_STATUS_TRANSITION.getCode());
    }

    @Test
    void attachDocumentRejectsBlankUrl() {
        KycProfile profile = draftProfile();
        when(kycPersistencePort.findByKycIdAndUserId(KYC_ID, USER_ID)).thenReturn(Optional.of(profile));

        IdentityException ex = assertThrows(IdentityException.class, () -> kycService.attachDocument(
                USER_ID, KYC_ID, "ID_FRONT", "  ", null));

        assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.KYC_DOCUMENT_REQUIRED.getCode());
    }

    @Test
    void listDocumentsReturnsPersistedDocuments() {
        KycProfile profile = draftProfile();
        KycDocument document = new KycDocument("doc-1", KYC_ID, KycDocumentType.ID_FRONT,
                "https://cdn.test/front.jpg", null, KycDocumentStatus.UPLOADED, Instant.now());
        when(kycPersistencePort.findByKycIdAndUserId(KYC_ID, USER_ID)).thenReturn(Optional.of(profile));
        when(kycPersistencePort.findDocumentsByKycId(KYC_ID)).thenReturn(List.of(document));

        List<KycDocument> result = kycService.listDocuments(USER_ID, KYC_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isSameAs(document);
    }

    @Test
    void submitRejectsExternallyManagedProfile() {
        KycProfile profile = submittedProfile();
        when(kycPersistencePort.findByKycIdAndUserId(KYC_ID, USER_ID)).thenReturn(Optional.of(profile));

        IdentityException ex = assertThrows(IdentityException.class,
                () -> kycService.submit(USER_ID, KYC_ID));

        assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.KYC_MANAGED_EXTERNALLY.getCode());
    }

    @Test
    void submitAcceptsPassportWithSinglePassportDocument() {
        KycProfile profile = new KycProfile(
                KYC_ID, USER_ID, "PASSPORT",
                null, KycStatus.DRAFT,
                null, null, null, null, null,
                null, null, null, null,
                null, null, Instant.now());
        when(kycPersistencePort.findByKycIdAndUserId(KYC_ID, USER_ID)).thenReturn(Optional.of(profile));
        when(kycPersistencePort.findDocumentsByKycId(KYC_ID)).thenReturn(List.of(
                new KycDocument("doc-passport", KYC_ID, KycDocumentType.PASSPORT,
                        "https://cdn.test/passport.jpg", null, KycDocumentStatus.UPLOADED, Instant.now())));
        when(kycPersistencePort.save(profile)).thenReturn(profile);

        KycProfile result = kycService.submit(USER_ID, KYC_ID);

        assertThat(result.getStatus()).isEqualTo(KycStatus.SUBMITTED);
    }

    @Test
    void submitRejectsWhenNoDocumentsUploaded() {
        KycProfile profile = draftProfile();
        when(kycPersistencePort.findByKycIdAndUserId(KYC_ID, USER_ID)).thenReturn(Optional.of(profile));
        when(kycPersistencePort.findDocumentsByKycId(KYC_ID)).thenReturn(List.of());

        IdentityException ex = assertThrows(IdentityException.class,
                () -> kycService.submit(USER_ID, KYC_ID));

        assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.KYC_DOCUMENT_REQUIRED.getCode());
    }

    @Test
    void generateVerificationSessionThrowsWhenManagedProviderDisabled() {
        when(kycPolicy.usesManagedProvider()).thenReturn(false);

        IdentityException ex = assertThrows(IdentityException.class,
                () -> kycService.generateVerificationSession(USER_ID, KYC_ID));

        assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.KYC_MANAGED_EXTERNALLY.getCode());
    }

    @Test
    void generateVerificationSessionThrowsWhenProfileNotManaged() {
        when(kycPolicy.usesManagedProvider()).thenReturn(true);
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(kycPersistencePort.findByKycIdAndUserId(KYC_ID, USER_ID)).thenReturn(Optional.of(draftProfile()));

        IdentityException ex = assertThrows(IdentityException.class,
                () -> kycService.generateVerificationSession(USER_ID, KYC_ID));

        assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.KYC_PROVIDER_NOT_CONFIGURED.getCode());
    }

    @Test
    void generateVerificationSessionReturnsSessionForManagedProfile() {
        KycProfile profile = submittedProfile();
        when(kycPolicy.usesManagedProvider()).thenReturn(true);
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(kycPersistencePort.findByKycIdAndUserId(KYC_ID, USER_ID)).thenReturn(Optional.of(profile));
        when(externalKycVerificationPort.generateVerificationSession(any(), any(), any()))
                .thenReturn(new ExternalKycVerificationPort.ExternalKycSession(
                        "SUMSUB", "applicant-1", "basic", "token-xyz", 600, true));

        KycVerificationSessionResult result = kycService.generateVerificationSession(USER_ID, KYC_ID);

        assertThat(result.kycId()).isEqualTo(KYC_ID);
        assertThat(result.provider()).isEqualTo("SUMSUB");
        assertThat(result.sdkAccessToken()).isEqualTo("token-xyz");
        assertThat(result.expiresInSeconds()).isEqualTo(600);
        assertThat(result.sandbox()).isTrue();
    }

    @Test
    void handleSumsubWebhookIgnoredWhenProviderDisabled() {
        when(kycPolicy.isSumsubEnabled()).thenReturn(false);

        kycService.handleSumsubWebhook(webhookCommand("applicant-1", "GREEN"));

        verify(externalKycVerificationPort, never()).verifyWebhookSignature(any(), any(), any());
        verify(kycPersistencePort, never()).save(any());
    }

    @Test
    void handleSumsubWebhookThrowsWhenApplicantIdMissing() {
        when(kycPolicy.isSumsubEnabled()).thenReturn(true);

        IdentityException ex = assertThrows(IdentityException.class,
                () -> kycService.handleSumsubWebhook(webhookCommand("  ", "GREEN")));

        assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.KYC_PROVIDER_ERROR.getCode());
    }

    @Test
    void handleSumsubWebhookThrowsWhenProfileNotFound() {
        when(kycPolicy.isSumsubEnabled()).thenReturn(true);
        when(kycPersistencePort.findByProviderApplicantId("applicant-1")).thenReturn(Optional.empty());

        assertThrows(IdentityException.class,
                () -> kycService.handleSumsubWebhook(webhookCommand("applicant-1", "GREEN")));
    }

    @Test
    void handleSumsubWebhookApprovesProfileOnGreenAnswer() {
        KycProfile profile = submittedProfile();
        when(kycPolicy.isSumsubEnabled()).thenReturn(true);
        when(kycPersistencePort.findByProviderApplicantId("applicant-1")).thenReturn(Optional.of(profile));
        when(kycPersistencePort.save(profile)).thenReturn(profile);

        kycService.handleSumsubWebhook(webhookCommand("applicant-1", "GREEN"));

        assertThat(profile.getStatus()).isEqualTo(KycStatus.APPROVED);
        verify(kycPersistencePort).save(profile);
    }

    private static SumsubWebhookCommand webhookCommand(String applicantId, String reviewAnswer) {
        return new SumsubWebhookCommand(
                "payload".getBytes(),
                "digest",
                "HMAC_SHA256_HEX",
                applicantId,
                "completed",
                reviewAnswer,
                null,
                null,
                "corr-1");
    }
}
