package com.aionn.identity.application.service;

import com.aionn.identity.application.dto.kyc.command.SumsubWebhookCommand;
import com.aionn.identity.application.dto.kyc.result.KycVerificationSessionResult;
import com.aionn.identity.application.policy.KycPolicy;
import com.aionn.identity.application.port.out.kyc.ExternalKycVerificationPort;
import com.aionn.identity.application.port.out.kyc.KycPersistencePort;
import com.aionn.identity.application.port.out.user.UserPersistencePort;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.model.IdentityUser;
import com.aionn.identity.domain.model.KycDocument;
import com.aionn.identity.domain.model.KycProfile;
import com.aionn.identity.domain.valueobject.KycDocumentStatus;
import com.aionn.identity.domain.valueobject.KycDocumentType;
import com.aionn.identity.domain.valueobject.KycStatus;
import com.aionn.identity.domain.valueobject.KycReviewAnswer;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Clock;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class KycService {

    private static final Set<String> SUPPORTED_PROFILE_DOCUMENT_TYPES = Set.of("ID_CARD", "CCCD", "CMND", "PASSPORT");

    private final KycPersistencePort kycPersistencePort;
    private final UserPersistencePort userPersistencePort;
    private final KycPolicy kycPolicy;
    private final ExternalKycVerificationPort externalKycVerificationPort;

    private final Clock clock;
    @Transactional(readOnly = true)
    public KycCreationPlan planKycCreation(String userId, String docType) {
        log.info("Creating KYC profile for user: {}, docType: {}", userId, docType);
        IdentityUser user = requireUser(userId);
        String normalizedDocType = normalizeProfileDocumentType(docType);
        return new KycCreationPlan(
                IdGenerator.ulid(),
                user,
                normalizedDocType,
                kycPolicy.usesManagedProvider());
    }

    public KycProfile createKyc(
            KycCreationPlan plan,
            ExternalKycVerificationPort.ExternalKycApplicant applicant) {
        KycProfile kyc = new KycProfile(
                plan.kycId(),
                plan.user().getUserId(),
                plan.docType(),
                null,
                plan.managedProviderEnabled() ? KycStatus.SUBMITTED : KycStatus.DRAFT,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                plan.managedProviderEnabled() ? nowUtc() : null,
                null,
                nowUtc());

        if (!plan.managedProviderEnabled()) {
            return kycPersistencePort.save(kyc);
        }
        if (applicant == null) {
            throw new IdentityException(IdentityErrorCode.KYC_PROVIDER_ERROR, "Missing external KYC applicant");
        }
        kyc.attachExternalProvider(
                applicant.provider(),
                applicant.applicantId(),
                applicant.levelName(),
                applicant.reviewStatus(),
                applicant.correlationId());
        return kycPersistencePort.save(kyc);
    }

    @Transactional(readOnly = true)
    public List<KycProfile> listMy(String userId) {
        log.debug("Listing KYC profiles for user: {}", userId);
        validateUserExists(userId);
        return kycPersistencePort.findByUserIdOrderBySubmittedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public KycProfile get(String userId, String kycId) {
        log.debug("Getting KYC: {}, user: {}", kycId, userId);
        validateUserExists(userId);
        return getKycByUser(kycId, userId);
    }

    @Transactional(readOnly = true)
    public List<KycProfile> adminListByStatus(KycStatus status, int limit) {
        log.debug("Admin listing KYC profiles by status={}, limit={}", status, limit);
        return kycPersistencePort.findByStatus(status, limit);
    }

    @Transactional(readOnly = true)
    public KycProfile adminGet(String kycId) {
        return kycPersistencePort.findById(kycId)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.KYC_NOT_FOUND));
    }

    public KycProfile adminApprove(String kycId, String adminId, String note) {
        KycProfile kyc = kycPersistencePort.findById(kycId)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.KYC_NOT_FOUND));
        try {
            kyc.adminApprove(adminId, note, clock);
        } catch (IllegalStateException ex) {
            throw new IdentityException(IdentityErrorCode.KYC_INVALID_STATUS_TRANSITION, ex.getMessage());
        }
        return kycPersistencePort.save(kyc);
    }

    public KycProfile adminReject(String kycId, String adminId, String reason) {
        KycProfile kyc = kycPersistencePort.findById(kycId)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.KYC_NOT_FOUND));
        try {
            kyc.adminReject(adminId, reason);
        } catch (IllegalStateException ex) {
            throw new IdentityException(IdentityErrorCode.KYC_INVALID_STATUS_TRANSITION, ex.getMessage());
        }
        return kycPersistencePort.save(kyc);
    }

    public KycProfile adminMarkInReview(String kycId, String adminId, String note) {
        KycProfile kyc = kycPersistencePort.findById(kycId)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.KYC_NOT_FOUND));
        try {
            kyc.adminMarkInReview(adminId, note);
        } catch (IllegalStateException ex) {
            throw new IdentityException(IdentityErrorCode.KYC_INVALID_STATUS_TRANSITION, ex.getMessage());
        }
        return kycPersistencePort.save(kyc);
    }

    public KycDocument attachDocument(
            String userId,
            String kycId,
            String documentType,
            String url,
            String publicId) {
        KycProfile kyc = getKycByUser(kycId, userId);
        if (kyc.isManagedExternally()) {
            throw new IdentityException(IdentityErrorCode.KYC_MANAGED_EXTERNALLY,
                    "Documents are managed by the external KYC provider");
        }
        if (kyc.getStatus() != KycStatus.DRAFT && kyc.getStatus() != KycStatus.REJECTED) {
            throw new IdentityException(IdentityErrorCode.KYC_INVALID_STATUS_TRANSITION,
                    "KYC documents can only be attached before submission");
        }
        if (url == null || url.isBlank()) {
            throw new IdentityException(IdentityErrorCode.KYC_DOCUMENT_REQUIRED, "KYC document URL is required");
        }

        KycDocument document = new KycDocument(
                IdGenerator.ulid(),
                kycId,
                KycDocumentType.from(documentType),
                url,
                publicId,
                KycDocumentStatus.UPLOADED,
                nowUtc());
        KycDocument saved = kycPersistencePort.saveDocument(document);
        kyc.attachBlobUrlIfEmpty(url);
        kycPersistencePort.save(kyc);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<KycDocument> listDocuments(String userId, String kycId) {
        getKycByUser(kycId, userId);
        return kycPersistencePort.findDocumentsByKycId(kycId);
    }

    public KycProfile submit(String userId, String kycId) {
        KycProfile kyc = getKycByUser(kycId, userId);
        if (kyc.isManagedExternally()) {
            throw new IdentityException(IdentityErrorCode.KYC_MANAGED_EXTERNALLY,
                    "External KYC profiles are submitted by the provider flow");
        }
        List<KycDocument> documents = kycPersistencePort.findDocumentsByKycId(kycId);
        if (!hasRequiredDocuments(kyc.getDocType(), documents)) {
            throw new IdentityException(IdentityErrorCode.KYC_DOCUMENT_REQUIRED,
                    "Required KYC documents have not been uploaded");
        }
        try {
            kyc.submit(clock);
        } catch (IllegalStateException ex) {
            throw new IdentityException(IdentityErrorCode.KYC_INVALID_STATUS_TRANSITION, ex.getMessage());
        }
        return kycPersistencePort.save(kyc);
    }

    public KycVerificationSessionResult generateVerificationSession(String userId, String kycId) {
        if (!kycPolicy.usesManagedProvider()) {
            throw new IdentityException(IdentityErrorCode.KYC_MANAGED_EXTERNALLY,
                    "External KYC session is only available when a managed KYC provider is enabled");
        }

        IdentityUser user = requireUser(userId);
        KycProfile kyc = getKycByUser(kycId, userId);
        if (!kyc.isManagedExternally()) {
            throw new IdentityException(IdentityErrorCode.KYC_PROVIDER_NOT_CONFIGURED);
        }

        var session = externalKycVerificationPort.generateVerificationSession(
                user,
                kyc.getKycId(),
                kyc.getProviderApplicantId());
        return new KycVerificationSessionResult(
                kyc.getKycId(),
                session.provider(),
                session.applicantId(),
                session.levelName(),
                session.accessToken(),
                session.expiresInSeconds(),
                session.sandbox());
    }

    public void handleSumsubWebhook(SumsubWebhookCommand command) {
        if (!kycPolicy.isSumsubEnabled()) {
            log.info("Ignoring Sumsub webhook because provider is not enabled");
            return;
        }

        externalKycVerificationPort.verifyWebhookSignature(
                command.payload(),
                command.digest(),
                command.digestAlgorithm());

        if (command.providerApplicantId() == null || command.providerApplicantId().isBlank()) {
            throw new IdentityException(IdentityErrorCode.KYC_PROVIDER_ERROR, "Webhook is missing applicantId");
        }

        KycProfile kyc = kycPersistencePort.findByProviderApplicantId(command.providerApplicantId())
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.KYC_NOT_FOUND));
        kyc.syncExternalReview(
                command.providerReviewStatus(),
                command.correlationId(),
                KycReviewAnswer.from(command.reviewAnswer()),
                command.moderationComment(),
                command.clientComment());
        kycPersistencePort.save(kyc);
    }

    private KycProfile getKycByUser(String kycId, String userId) {
        return kycPersistencePort.findByKycIdAndUserId(kycId, userId)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.KYC_NOT_FOUND));
    }

    private void validateUserExists(String userId) {
        requireUser(userId);
    }

    private IdentityUser requireUser(String userId) {
        return userPersistencePort.findById(userId)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.USER_NOT_FOUND));
    }

    private boolean hasRequiredDocuments(String docType, List<KycDocument> documents) {
        if (documents.isEmpty()) {
            return false;
        }
        Set<KycDocumentType> uploadedTypes = EnumSet.noneOf(KycDocumentType.class);
        documents.forEach(document -> uploadedTypes.add(document.getType()));
        String normalizedDocType = normalizeProfileDocumentType(docType);
        if ("PASSPORT".equals(normalizedDocType)) {
            return uploadedTypes.contains(KycDocumentType.PASSPORT);
        }
        if ("ID_CARD".equals(normalizedDocType)
                || "CCCD".equals(normalizedDocType)
                || "CMND".equals(normalizedDocType)) {
            return uploadedTypes.contains(KycDocumentType.ID_FRONT)
                    && uploadedTypes.contains(KycDocumentType.ID_BACK);
        }
        return true;
    }

    private static String normalizeProfileDocumentType(String docType) {
        if (docType == null || docType.isBlank()) {
            throw new IdentityException(IdentityErrorCode.KYC_DOCUMENT_TYPE_INVALID);
        }
        String normalized = docType.trim().toUpperCase();
        if (!SUPPORTED_PROFILE_DOCUMENT_TYPES.contains(normalized)) {
            throw new IdentityException(IdentityErrorCode.KYC_DOCUMENT_TYPE_INVALID);
        }
        return normalized;
    }

    public record KycCreationPlan(
            String kycId,
            IdentityUser user,
            String docType,
            boolean managedProviderEnabled) {
    }
    private Instant nowUtc() {
        return clock.instant();
    }
}
