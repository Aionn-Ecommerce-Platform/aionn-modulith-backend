package com.aionn.identity.infrastructure.persistence.repository.kyc;

import com.aionn.identity.infrastructure.persistence.entity.KycDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KycDocumentRepository extends JpaRepository<KycDocumentEntity, String> {

    List<KycDocumentEntity> findByKyc_KycIdOrderByUploadedAtAsc(String kycId);
}
