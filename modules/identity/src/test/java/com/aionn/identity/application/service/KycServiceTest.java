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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
                kycPersistencePort, userPersistencePort, kycPolicy, externalKycVerificationPort);
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
                null, null, LocalDateTime.now());
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

        assertEquals(USER_ID, plan.user().getUserId());
        assertEquals("CCCD", plan.docType());
        assertEquals(false, plan.managedProviderEnabled());
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

        assertEquals(KycStatus.DRAFT, created.getStatus());
        verify(externalKycVerificationPort, never()).createApplicant(any(), any(), any());
    }

    @Test
    void createKycRejectsUnknownDocumentType() {
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));

        IdentityException ex = assertThrows(IdentityException.class,
                () -> kycService.planKycCreation(USER_ID, "DRIVER_LICENSE"));

        assertEquals(IdentityErrorCode.KYC_DOCUMENT_TYPE_INVALID.getCode(), ex.getErrorCode());
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

        assertEquals(KycStatus.SUBMITTED, created.getStatus());
        assertEquals("SUMSUB", created.getProvider());
        assertEquals("applicant-9", created.getProviderApplicantId());
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
                LocalDateTime.now());
        when(kycPersistencePort.findByKycIdAndUserId(KYC_ID, USER_ID)).thenReturn(Optional.of(profile));
        when(kycPersistencePort.saveDocument(any(KycDocument.class))).thenReturn(savedDocument);
        when(kycPersistencePort.save(profile)).thenReturn(profile);

        KycDocument result = kycService.attachDocument(
                USER_ID,
                KYC_ID,
                "ID_FRONT",
                "https://cdn.test/front.jpg",
                "identity/kyc/front");

        assertSame(savedDocument, result);
        assertEquals("https://cdn.test/front.jpg", profile.getBlobUrl());
        verify(kycPersistencePort).save(profile);
    }

    @Test
    void submitRequiresFrontAndBackForIdCard() {
        KycProfile profile = draftProfile();
        when(kycPersistencePort.findByKycIdAndUserId(KYC_ID, USER_ID)).thenReturn(Optional.of(profile));
        when(kycPersistencePort.findDocumentsByKycId(KYC_ID)).thenReturn(List.of(
                new KycDocument("doc-front", KYC_ID, KycDocumentType.ID_FRONT,
                        "https://cdn.test/front.jpg", null, KycDocumentStatus.UPLOADED, LocalDateTime.now())));

        assertThrows(IdentityException.class, () -> kycService.submit(USER_ID, KYC_ID));
    }

    @Test
    void submitMovesLocalProfileToSubmittedWhenRequiredDocumentsExist() {
        KycProfile profile = draftProfile();
        when(kycPersistencePort.findByKycIdAndUserId(KYC_ID, USER_ID)).thenReturn(Optional.of(profile));
        when(kycPersistencePort.findDocumentsByKycId(KYC_ID)).thenReturn(List.of(
                new KycDocument("doc-front", KYC_ID, KycDocumentType.ID_FRONT,
                        "https://cdn.test/front.jpg", null, KycDocumentStatus.UPLOADED, LocalDateTime.now()),
                new KycDocument("doc-back", KYC_ID, KycDocumentType.ID_BACK,
                        "https://cdn.test/back.jpg", null, KycDocumentStatus.UPLOADED, LocalDateTime.now())));
        when(kycPersistencePort.save(profile)).thenReturn(profile);

        KycProfile result = kycService.submit(USER_ID, KYC_ID);

        assertSame(profile, result);
        assertEquals(KycStatus.SUBMITTED, result.getStatus());
        assertNotNull(result.getSubmittedAt());
    }

    @Test
    void adminApproveTransitionsSubmittedToApproved() {
        KycProfile profile = submittedProfile();
        when(kycPersistencePort.findById(KYC_ID)).thenReturn(Optional.of(profile));
        when(kycPersistencePort.save(profile)).thenReturn(profile);

        KycProfile result = kycService.adminApprove(KYC_ID, "admin-1", "ok");

        assertEquals(KycStatus.APPROVED, result.getStatus());
        assertEquals("admin-1", result.getDecisionAdminId());
        assertNotNull(result.getApprovedAt());
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

        assertEquals(1, result.size());
        assertSame(p, result.get(0));
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

        assertEquals(IdentityErrorCode.USER_NOT_FOUND.getCode(), ex.getErrorCode());
    }

    @Test
    void createKycThrowsWhenManagedProviderApplicantMissing() {
        KycService.KycCreationPlan plan = new KycService.KycCreationPlan(
                KYC_ID, activeUser(), "ID_CARD", true);

        IdentityException ex = assertThrows(IdentityException.class,
                () -> kycService.createKyc(plan, null));

        assertEquals(IdentityErrorCode.KYC_PROVIDER_ERROR.getCode(), ex.getErrorCode());
        verify(kycPersistencePort, never()).save(any());
    }

    @Test
    void getReturnsProfileForOwner() {
        KycProfile profile = draftProfile();
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(kycPersistencePort.findByKycIdAndUserId(KYC_ID, USER_ID)).thenReturn(Optional.of(profile));

        assertSame(profile, kycService.get(USER_ID, KYC_ID));
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

        assertEquals(1, result.size());
        assertSame(p, result.get(0));
    }

    @Test
    void adminGetReturnsProfile() {
        KycProfile p = submittedProfile();
        when(kycPersistencePort.findById(KYC_ID)).thenReturn(Optional.of(p));

        assertSame(p, kycService.adminGet(KYC_ID));
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

        assertEquals(KycStatus.REJECTED, result.getStatus());
        assertEquals("blurry document", result.getRejectReason());
    }

    @Test
    void adminMarkInReviewTransitionsSubmittedToInReview() {
        KycProfile profile = submittedProfile();
        when(kycPersistencePort.findById(KYC_ID)).thenReturn(Optional.of(profile));
        when(kycPersistencePort.save(profile)).thenReturn(profile);

        KycProfile result = kycService.adminMarkInReview(KYC_ID, "admin-1", "manual check");

        assertEquals(KycStatus.IN_REVIEW, result.getStatus());
        assertEquals("admin-1", result.getReviewerId());
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

        assertEquals(IdentityErrorCode.KYC_MANAGED_EXTERNALLY.getCode(), ex.getErrorCode());
        verify(kycPersistencePort, never()).saveDocument(any());
    }

    @Test
    void attachDocumentRejectsWhenProfileAlreadySubmitted() {
        KycProfile profile = draftProfile();
        profile.submit();
        when(kycPersistencePort.findByKycIdAndUserId(KYC_ID, USER_ID)).thenReturn(Optional.of(profile));

        IdentityException ex = assertThrows(IdentityException.class, () -> kycService.attachDocument(
                USER_ID, KYC_ID, "ID_FRONT", "https://cdn.test/front.jpg", null));

        assertEquals(IdentityErrorCode.KYC_INVALID_STATUS_TRANSITION.getCode(), ex.getErrorCode());
    }

    @Test
    void attachDocumentRejectsBlankUrl() {
        KycProfile profile = draftProfile();
        when(kycPersistencePort.findByKycIdAndUserId(KYC_ID, USER_ID)).thenReturn(Optional.of(profile));

        IdentityException ex = assertThrows(IdentityException.class, () -> kycService.attachDocument(
                USER_ID, KYC_ID, "ID_FRONT", "  ", null));

        assertEquals(IdentityErrorCode.KYC_DOCUMENT_REQUIRED.getCode(), ex.getErrorCode());
    }

    @Test
    void listDocumentsReturnsPersistedDocuments() {
        KycProfile profile = draftProfile();
        KycDocument document = new KycDocument("doc-1", KYC_ID, KycDocumentType.ID_FRONT,
                "https://cdn.test/front.jpg", null, KycDocumentStatus.UPLOADED, LocalDateTime.now());
        when(kycPersistencePort.findByKycIdAndUserId(KYC_ID, USER_ID)).thenReturn(Optional.of(profile));
        when(kycPersistencePort.findDocumentsByKycId(KYC_ID)).thenReturn(List.of(document));

        List<KycDocument> result = kycService.listDocuments(USER_ID, KYC_ID);

        assertEquals(1, result.size());
        assertSame(document, result.get(0));
    }

    @Test
    void submitRejectsExternallyManagedProfile() {
        KycProfile profile = submittedProfile();
        when(kycPersistencePort.findByKycIdAndUserId(KYC_ID, USER_ID)).thenReturn(Optional.of(profile));

        IdentityException ex = assertThrows(IdentityException.class,
                () -> kycService.submit(USER_ID, KYC_ID));

        assertEquals(IdentityErrorCode.KYC_MANAGED_EXTERNALLY.getCode(), ex.getErrorCode());
    }

    @Test
    void submitAcceptsPassportWithSinglePassportDocument() {
        KycProfile profile = new KycProfile(
                KYC_ID, USER_ID, "PASSPORT",
                null, KycStatus.DRAFT,
                null, null, null, null, null,
                null, null, null, null,
                null, null, LocalDateTime.now());
        when(kycPersistencePort.findByKycIdAndUserId(KYC_ID, USER_ID)).thenReturn(Optional.of(profile));
        when(kycPersistencePort.findDocumentsByKycId(KYC_ID)).thenReturn(List.of(
                new KycDocument("doc-passport", KYC_ID, KycDocumentType.PASSPORT,
                        "https://cdn.test/passport.jpg", null, KycDocumentStatus.UPLOADED, LocalDateTime.now())));
        when(kycPersistencePort.save(profile)).thenReturn(profile);

        KycProfile result = kycService.submit(USER_ID, KYC_ID);

        assertEquals(KycStatus.SUBMITTED, result.getStatus());
    }

    @Test
    void submitRejectsWhenNoDocumentsUploaded() {
        KycProfile profile = draftProfile();
        when(kycPersistencePort.findByKycIdAndUserId(KYC_ID, USER_ID)).thenReturn(Optional.of(profile));
        when(kycPersistencePort.findDocumentsByKycId(KYC_ID)).thenReturn(List.of());

        IdentityException ex = assertThrows(IdentityException.class,
                () -> kycService.submit(USER_ID, KYC_ID));

        assertEquals(IdentityErrorCode.KYC_DOCUMENT_REQUIRED.getCode(), ex.getErrorCode());
    }

    @Test
    void generateVerificationSessionThrowsWhenManagedProviderDisabled() {
        when(kycPolicy.usesManagedProvider()).thenReturn(false);

        IdentityException ex = assertThrows(IdentityException.class,
                () -> kycService.generateVerificationSession(USER_ID, KYC_ID));

        assertEquals(IdentityErrorCode.KYC_MANAGED_EXTERNALLY.getCode(), ex.getErrorCode());
    }

    @Test
    void generateVerificationSessionThrowsWhenProfileNotManaged() {
        when(kycPolicy.usesManagedProvider()).thenReturn(true);
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(kycPersistencePort.findByKycIdAndUserId(KYC_ID, USER_ID)).thenReturn(Optional.of(draftProfile()));

        IdentityException ex = assertThrows(IdentityException.class,
                () -> kycService.generateVerificationSession(USER_ID, KYC_ID));

        assertEquals(IdentityErrorCode.KYC_PROVIDER_NOT_CONFIGURED.getCode(), ex.getErrorCode());
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

        assertEquals(KYC_ID, result.kycId());
        assertEquals("SUMSUB", result.provider());
        assertEquals("token-xyz", result.sdkAccessToken());
        assertEquals(600, result.expiresInSeconds());
        assertTrue(result.sandbox());
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

        assertEquals(IdentityErrorCode.KYC_PROVIDER_ERROR.getCode(), ex.getErrorCode());
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

        assertEquals(KycStatus.APPROVED, profile.getStatus());
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
