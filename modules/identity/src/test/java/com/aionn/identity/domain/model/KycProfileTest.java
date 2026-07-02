package com.aionn.identity.domain.model;

import com.aionn.identity.domain.valueobject.KycReviewAnswer;
import com.aionn.identity.domain.valueobject.KycStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KycProfileTest {

    private static KycProfile draft() {
        return new KycProfile(
                "kyc-1", "user-1", "ID_CARD",
                null, KycStatus.DRAFT,
                null, null, null, null, null,
                null, null, null, null,
                null, null, LocalDateTime.now());
    }

    private static KycProfile submitted() {
        KycProfile k = draft();
        k.attachExternalProvider("SUMSUB", "app-1", "basic", "init", "corr-1");
        return k;
    }

    @Test
    void attachExternalProviderMovesDraftToSubmitted() {
        KycProfile kyc = draft();

        kyc.attachExternalProvider("SUMSUB", "app-1", "basic-kyc", "init", "corr-1");

        assertEquals(KycStatus.SUBMITTED, kyc.getStatus());
        assertEquals("SUMSUB", kyc.getProvider());
        assertEquals("app-1", kyc.getProviderApplicantId());
        assertNotNull(kyc.getSubmittedAt());
        assertTrue(kyc.isManagedExternally());
    }

    @Test
    void adminApproveTransitionsSubmittedToApproved() {
        KycProfile kyc = submitted();

        kyc.adminApprove("admin-1", "looks good");

        assertEquals(KycStatus.APPROVED, kyc.getStatus());
        assertEquals("admin-1", kyc.getDecisionAdminId());
        assertEquals("admin-1", kyc.getReviewerId());
        assertNotNull(kyc.getApprovedAt());
        assertNull(kyc.getRejectReason());
    }

    @Test
    void adminApproveFromDraftThrows() {
        KycProfile kyc = draft();

        assertThrows(IllegalStateException.class, () -> kyc.adminApprove("admin-1", "n"));
    }

    @Test
    void submitMovesDraftToSubmitted() {
        KycProfile kyc = draft();

        kyc.submit();

        assertEquals(KycStatus.SUBMITTED, kyc.getStatus());
        assertNotNull(kyc.getSubmittedAt());
    }

    @Test
    void submitMovesRejectedToSubmitted() {
        KycProfile kyc = submitted();
        kyc.adminReject("admin-1", "blurred");

        kyc.submit();

        assertEquals(KycStatus.SUBMITTED, kyc.getStatus());
        assertNull(kyc.getRejectReason());
        assertNull(kyc.getApprovedAt());
    }

    @Test
    void attachBlobUrlKeepsFirstDocumentUrl() {
        KycProfile kyc = draft();

        kyc.attachBlobUrlIfEmpty("https://cdn.test/front.jpg");
        kyc.attachBlobUrlIfEmpty("https://cdn.test/back.jpg");

        assertEquals("https://cdn.test/front.jpg", kyc.getBlobUrl());
    }

    @Test
    void adminRejectStoresReason() {
        KycProfile kyc = submitted();

        kyc.adminReject("admin-1", "bad photo");

        assertEquals(KycStatus.REJECTED, kyc.getStatus());
        assertEquals("bad photo", kyc.getRejectReason());
        assertNull(kyc.getApprovedAt());
    }

    @Test
    void syncExternalReviewGreenApproves() {
        KycProfile kyc = submitted();

        kyc.syncExternalReview("completed", "corr-2", KycReviewAnswer.GREEN, "ok", null);

        assertEquals(KycStatus.APPROVED, kyc.getStatus());
        assertEquals("SUMSUB", kyc.getDecisionAdminId());
        assertNotNull(kyc.getApprovedAt());
    }

    @Test
    void syncExternalReviewRedRejects() {
        KycProfile kyc = submitted();

        kyc.syncExternalReview("completed", "corr-3", KycReviewAnswer.RED, "fraud", "user msg");

        assertEquals(KycStatus.REJECTED, kyc.getStatus());
        assertEquals("fraud", kyc.getRejectReason());
    }

    @Test
    void syncExternalReviewCannotOverrideApprovedWithRejected() {
        KycProfile kyc = submitted();
        kyc.syncExternalReview("completed", "corr-2", KycReviewAnswer.GREEN, "ok", null);

        assertThrows(IllegalStateException.class,
                () -> kyc.syncExternalReview("completed", "corr-3", KycReviewAnswer.RED, "fraud", null));
    }
}
