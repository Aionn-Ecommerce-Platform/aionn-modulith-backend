package com.aionn.identity.domain.model;

import com.aionn.identity.domain.valueobject.KycReviewAnswer;
import com.aionn.identity.domain.valueobject.KycStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KycProfileTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC);

    private static KycProfile draft() {
        return new KycProfile(
                "kyc-1", "user-1", "ID_CARD",
                null, KycStatus.DRAFT,
                null, null, null, null, null,
                null, null, null, null,
                null, null, Instant.now(CLOCK));
    }

    private static KycProfile submitted() {
        KycProfile k = draft();
        k.attachExternalProvider("SUMSUB", "app-1", "basic", "init", "corr-1", CLOCK);
        return k;
    }

    @Test
    void attachExternalProviderMovesDraftToSubmitted() {
        KycProfile kyc = draft();

        kyc.attachExternalProvider("SUMSUB", "app-1", "basic-kyc", "init", "corr-1", CLOCK);

        assertThat(kyc.getStatus()).isEqualTo(KycStatus.SUBMITTED);
        assertThat(kyc.getProvider()).isEqualTo("SUMSUB");
        assertThat(kyc.getProviderApplicantId()).isEqualTo("app-1");
        assertThat(kyc.getSubmittedAt()).isNotNull();
        assertThat(kyc.isManagedExternally()).isTrue();
    }

    @Test
    void adminApproveTransitionsSubmittedToApproved() {
        KycProfile kyc = submitted();

        kyc.adminApprove("admin-1", "looks good", CLOCK);

        assertThat(kyc.getStatus()).isEqualTo(KycStatus.APPROVED);
        assertThat(kyc.getDecisionAdminId()).isEqualTo("admin-1");
        assertThat(kyc.getReviewerId()).isEqualTo("admin-1");
        assertThat(kyc.getApprovedAt()).isNotNull();
        assertThat(kyc.getRejectReason()).isNull();
    }

    @Test
    void adminApproveFromDraftThrows() {
        KycProfile kyc = draft();

        assertThrows(IllegalStateException.class, () -> kyc.adminApprove("admin-1", "n", CLOCK));
    }

    @Test
    void submitMovesDraftToSubmitted() {
        KycProfile kyc = draft();

        kyc.submit(CLOCK);

        assertThat(kyc.getStatus()).isEqualTo(KycStatus.SUBMITTED);
        assertThat(kyc.getSubmittedAt()).isNotNull();
    }

    @Test
    void submitMovesRejectedToSubmitted() {
        KycProfile kyc = submitted();
        kyc.adminReject("admin-1", "blurred");

        kyc.submit(CLOCK);

        assertThat(kyc.getStatus()).isEqualTo(KycStatus.SUBMITTED);
        assertThat(kyc.getRejectReason()).isNull();
        assertThat(kyc.getApprovedAt()).isNull();
    }

    @Test
    void attachBlobUrlKeepsFirstDocumentUrl() {
        KycProfile kyc = draft();

        kyc.attachBlobUrlIfEmpty("https://cdn.test/front.jpg");
        kyc.attachBlobUrlIfEmpty("https://cdn.test/back.jpg");

        assertThat(kyc.getBlobUrl()).isEqualTo("https://cdn.test/front.jpg");
    }

    @Test
    void adminRejectStoresReason() {
        KycProfile kyc = submitted();

        kyc.adminReject("admin-1", "bad photo");

        assertThat(kyc.getStatus()).isEqualTo(KycStatus.REJECTED);
        assertThat(kyc.getRejectReason()).isEqualTo("bad photo");
        assertThat(kyc.getApprovedAt()).isNull();
    }

    @Test
    void syncExternalReviewGreenApproves() {
        KycProfile kyc = submitted();

        kyc.syncExternalReview("completed", "corr-2", KycReviewAnswer.GREEN, "ok", null, CLOCK);

        assertThat(kyc.getStatus()).isEqualTo(KycStatus.APPROVED);
        assertThat(kyc.getDecisionAdminId()).isEqualTo("SUMSUB");
        assertThat(kyc.getApprovedAt()).isNotNull();
    }

    @Test
    void syncExternalReviewRedRejects() {
        KycProfile kyc = submitted();

        kyc.syncExternalReview("completed", "corr-3", KycReviewAnswer.RED, "fraud", "user msg", CLOCK);

        assertThat(kyc.getStatus()).isEqualTo(KycStatus.REJECTED);
        assertThat(kyc.getRejectReason()).isEqualTo("fraud");
    }

    @Test
    void syncExternalReviewCannotOverrideApprovedWithRejected() {
        KycProfile kyc = submitted();
        kyc.syncExternalReview("completed", "corr-2", KycReviewAnswer.GREEN, "ok", null, CLOCK);

        assertThrows(IllegalStateException.class,
                () -> kyc.syncExternalReview("completed", "corr-3", KycReviewAnswer.RED, "fraud", null, CLOCK));
    }
}
