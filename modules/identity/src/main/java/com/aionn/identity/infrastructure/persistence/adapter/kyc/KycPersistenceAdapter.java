package com.aionn.identity.infrastructure.persistence.adapter.kyc;

import com.aionn.identity.application.port.out.kyc.KycPersistencePort;
import com.aionn.identity.domain.model.KycDocument;
import com.aionn.identity.domain.model.KycProfile;
import com.aionn.identity.domain.valueobject.KycStatus;
import com.aionn.identity.infrastructure.persistence.entity.KycDocumentEntity;
import com.aionn.identity.infrastructure.persistence.entity.KycProfileEntity;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import com.aionn.identity.infrastructure.persistence.mapper.KycDocumentDomainMapper;
import com.aionn.identity.infrastructure.persistence.mapper.KycDomainMapper;
import com.aionn.identity.infrastructure.persistence.repository.kyc.KycDocumentRepository;
import com.aionn.identity.infrastructure.persistence.repository.kyc.KycProfileRepository;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class KycPersistenceAdapter implements KycPersistencePort {

    private final KycProfileRepository kycRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final UserRepository userRepository;
    private final KycDomainMapper mapper;
    private final KycDocumentDomainMapper documentMapper;

    @Override
    public KycProfile save(KycProfile kycProfile) {
        Optional<KycProfileEntity> managed = kycRepository.findById(kycProfile.getKycId());
        if (managed.isPresent()) {
            KycProfileEntity existing = managed.get();
            mapper.updateEntity(existing, kycProfile);
            KycProfileEntity saved = kycRepository.save(existing);
            return mapper.toDomain(saved);
        }
        KycProfileEntity entity = mapper.toEntity(kycProfile);
        UserEntity userRef = userRepository.getReferenceById(kycProfile.getUserId());
        entity.setUser(userRef);
        KycProfileEntity saved = kycRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<KycProfile> findByKycIdAndUserId(String kycId, String userId) {
        return kycRepository.findByKycIdAndUser_UserId(kycId, userId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<KycProfile> findById(String kycId) {
        return kycRepository.findById(kycId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<KycProfile> findByProviderApplicantId(String providerApplicantId) {
        return kycRepository.findByProviderApplicantId(providerApplicantId)
                .map(mapper::toDomain);
    }

    @Override
    public List<KycProfile> findByUserIdOrderBySubmittedAtDesc(String userId) {
        return kycRepository.findByUser_UserIdOrderBySubmittedAtDesc(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<KycProfile> findByStatus(KycStatus status, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return kycRepository
                .findByStatusOrderBySubmittedAtDesc(status.name(), PageRequest.of(0, safeLimit))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public KycDocument saveDocument(KycDocument document) {
        KycDocumentEntity entity = documentMapper.toEntity(document);
        entity.setKyc(kycRepository.getReferenceById(document.getKycId()));
        KycDocumentEntity saved = kycDocumentRepository.save(entity);
        return documentMapper.toDomain(saved);
    }

    @Override
    public List<KycDocument> findDocumentsByKycId(String kycId) {
        return kycDocumentRepository.findByKyc_KycIdOrderByUploadedAtAsc(kycId).stream()
                .map(documentMapper::toDomain)
                .toList();
    }

    @Override
    public void delete(KycProfile kycProfile) {
        kycRepository.deleteById(kycProfile.getKycId());
    }
}

