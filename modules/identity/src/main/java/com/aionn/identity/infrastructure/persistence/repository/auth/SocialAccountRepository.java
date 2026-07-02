package com.aionn.identity.infrastructure.persistence.repository.auth;

import com.aionn.identity.infrastructure.persistence.entity.SocialAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccountEntity, String> {

    Optional<SocialAccountEntity> findByUser_UserIdAndProvider(String userId, String provider);

    Optional<SocialAccountEntity> findByProviderAndProviderUserId(String provider, String providerUserId);

    boolean existsByProviderAndProviderUserId(String provider, String providerUserId);

    void deleteByUser_UserIdAndProvider(String userId, String provider);
}
