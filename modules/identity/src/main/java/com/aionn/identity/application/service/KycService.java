package com.aionn.identity.application.service;

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

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class KycService {

    private final KycPersistencePort kycPersistencePort;
    private final UserPersistencePort userPersistencePort;
    private final KycPolicy kycPolicy;
    private final ExternalKycVerificationPort externalKycVerificationPort;

    public KycProfile createKyc(String userId, String docType) {
        log.info("Creating KYC profile for user: {}, docType: {}", userId, docType);
        IdentityUser user = requireUser(userId);
        boolean managedProviderEnabled = kycPolicy.usesManagedProvider();

        KycProfile kyc = new KycProfile(
                IdGenerator.ulid(),
                userId,
                docType,
                null,
                managedProviderEnabled ? KycStatus.SUBMITTED : KycStatus.DRAFT,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                managedProviderEnabled ? LocalDateTime.now() : null,
                null,
                LocalDateTime.now());

        if (!managedProviderEnabled) {
            return kycPersistencePort.save(kyc);
        }

        var applicant = externalKycVerificationPort.createApplicant(user, kyc.getKycId(), docType);
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
            kyc.adminApprove(adminId, note);
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
                LocalDateTime.now());
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
            kyc.submit();
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

    public void handleSumsubWebhook(
            byte[] payload,
            String digest,
            String digestAlgorithm,
            String providerApplicantId,
            String providerReviewStatus,
            String reviewAnswer,
            String moderationComment,
            String clientComment,
            String correlationId) {
        if (!kycPolicy.isSumsubEnabled()) {
            log.info("Ignoring Sumsub webhook because provider is not enabled");
            return;
        }

        externalKycVerificationPort.verifyWebhookSignature(payload, digest, digestAlgorithm);

        if (providerApplicantId == null || providerApplicantId.isBlank()) {
            throw new IdentityException(IdentityErrorCode.KYC_PROVIDER_ERROR, "Webhook is missing applicantId");
        }

        KycProfile kyc = kycPersistencePort.findByProviderApplicantId(providerApplicantId)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.KYC_NOT_FOUND));
        kyc.syncExternalReview(
                providerReviewStatus,
                correlationId,
                KycReviewAnswer.from(reviewAnswer),
                moderationComment,
                clientComment);
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
        String normalizedDocType = docType == null ? "" : docType.trim().toUpperCase();
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

}
