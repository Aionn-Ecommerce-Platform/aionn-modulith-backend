package com.aionn.identity.application.service;

import com.aionn.identity.application.policy.KycPolicy;
import com.aionn.identity.application.port.out.kyc.ExternalKycVerificationPort;
import com.aionn.identity.application.port.out.kyc.KycPersistencePort;
import com.aionn.identity.application.port.out.user.UserPersistencePort;
import com.aionn.identity.domain.exception.IdentityException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KycServiceTest {

    private static final String USER_ID = "user-1";
    private static final String KYC_ID = "kyc-1";

    @Mock private KycPersistencePort kycPersistencePort;
    @Mock private UserPersistencePort userPersistencePort;
    @Mock private KycPolicy kycPolicy;
    @Mock private ExternalKycVerificationPort externalKycVerificationPort;

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
    void createKycInDraftWhenManagedProviderDisabled() {
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(kycPolicy.usesManagedProvider()).thenReturn(false);
        when(kycPersistencePort.save(any(KycProfile.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        KycProfile created = kycService.createKyc(USER_ID, "ID_CARD");

        assertEquals(KycStatus.DRAFT, created.getStatus());
        verify(externalKycVerificationPort, never()).createApplicant(any(), any(), any());
    }

    @Test
    void createKycAttachesExternalProviderWhenManaged() {
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(kycPolicy.usesManagedProvider()).thenReturn(true);
        when(externalKycVerificationPort.createApplicant(any(), any(), any()))
                .thenReturn(new ExternalKycVerificationPort.ExternalKycApplicant(
                        "SUMSUB", "applicant-9", "basic", "init", "corr-1"));
        when(kycPersistencePort.save(any(KycProfile.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        KycProfile created = kycService.createKyc(USER_ID, "ID_CARD");

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
}
