package com.aionn.identity.application.port.out.social;

import com.aionn.identity.domain.model.SocialLink;
import com.aionn.identity.domain.valueobject.AuthProvider;

import java.util.Optional;

public interface SocialLinkPersistencePort {
    Optional<SocialLink> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);

    Optional<SocialLink> findByUserIdAndProvider(String userId, AuthProvider provider);

    boolean existsByProviderAndProviderUserId(AuthProvider provider, String providerUserId);

    SocialLink save(SocialLink socialLink, String userId);

    void deleteByUserIdAndProvider(String userId, AuthProvider provider);
}
