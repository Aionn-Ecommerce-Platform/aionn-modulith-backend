package com.aionn.identity.application.port.out.kyc;

import com.aionn.identity.domain.model.KycProfile;
import com.aionn.identity.domain.model.KycDocument;
import com.aionn.identity.domain.valueobject.KycStatus;

import java.util.List;
import java.util.Optional;

public interface KycPersistencePort {
    KycProfile save(KycProfile kycProfile);

    Optional<KycProfile> findByKycIdAndUserId(String kycId, String userId);

    Optional<KycProfile> findById(String kycId);

    Optional<KycProfile> findByProviderApplicantId(String providerApplicantId);

    List<KycProfile> findByUserIdOrderBySubmittedAtDesc(String userId);

    List<KycProfile> findByStatus(KycStatus status, int limit);

    KycDocument saveDocument(KycDocument document);

    List<KycDocument> findDocumentsByKycId(String kycId);

    void delete(KycProfile kycProfile);
}
