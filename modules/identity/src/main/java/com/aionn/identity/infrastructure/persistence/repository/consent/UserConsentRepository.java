package com.aionn.identity.infrastructure.persistence.repository.consent;

import com.aionn.identity.infrastructure.persistence.entity.UserConsentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserConsentRepository extends JpaRepository<UserConsentEntity, String> {

    List<UserConsentEntity> findByUser_UserIdOrderByAgreedAtDesc(String userId);

    Optional<UserConsentEntity> findTopByUser_UserIdAndConsentTypeOrderByAgreedAtDesc(String userId, String consentType);
}



