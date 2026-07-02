package com.aionn.identity.infrastructure.persistence.adapter.social;

import com.aionn.identity.application.port.out.social.SocialLinkPersistencePort;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.model.SocialLink;
import com.aionn.identity.domain.valueobject.AuthProvider;
import com.aionn.identity.infrastructure.persistence.mapper.SocialLinkDomainMapper;
import com.aionn.identity.infrastructure.persistence.repository.auth.SocialAccountRepository;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SocialLinkPersistenceAdapter implements SocialLinkPersistencePort {

    private final SocialAccountRepository socialAccountRepository;
    private final UserRepository userRepository;
    private final SocialLinkDomainMapper socialLinkDomainMapper;

    @Override
    public Optional<SocialLink> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId) {
        return socialAccountRepository.findByProviderAndProviderUserId(provider.name(), providerUserId)
                .map(socialLinkDomainMapper::toDomain);
    }

    @Override
    public Optional<SocialLink> findByUserIdAndProvider(String userId, AuthProvider provider) {
        return socialAccountRepository.findByUser_UserIdAndProvider(userId, provider.name())
                .map(socialLinkDomainMapper::toDomain);
    }

    @Override
    public boolean existsByProviderAndProviderUserId(AuthProvider provider, String providerUserId) {
        return socialAccountRepository.existsByProviderAndProviderUserId(provider.name(), providerUserId);
    }

    @Override
    public SocialLink save(SocialLink socialLink, String userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.USER_NOT_FOUND));
        var entity = socialLinkDomainMapper.toEntity(socialLink, user);
        var savedEntity = socialAccountRepository.save(entity);
        return socialLinkDomainMapper.toDomain(savedEntity);
    }

    @Override
    public void deleteByUserIdAndProvider(String userId, AuthProvider provider) {
        socialAccountRepository.deleteByUser_UserIdAndProvider(userId, provider.name());
    }
}
