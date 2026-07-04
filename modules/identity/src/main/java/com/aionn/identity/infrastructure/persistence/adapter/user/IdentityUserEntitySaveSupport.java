package com.aionn.identity.infrastructure.persistence.adapter.user;

import com.aionn.identity.domain.model.IdentityUser;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import com.aionn.identity.infrastructure.persistence.mapper.IdentityUserMapper;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class IdentityUserEntitySaveSupport {

    private final UserRepository userRepository;
    private final IdentityUserMapper identityUserMapper;

    IdentityUser save(IdentityUser user) {
        UserEntity entity = userRepository.findById(user.getUserId())
                .orElseGet(() -> UserEntity.builder()
                        .userId(user.getUserId())
                        .roles(new java.util.LinkedHashSet<>())
                        .build());

        entity.setEmail(user.getEmail());
        entity.setPhone(user.getPhone());
        entity.setUsername(user.getUsername());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setDisplayName(user.getDisplayName());
        entity.setAvatarUrl(user.getAvatarUrl());
        entity.setStatus(user.getStatus());
        entity.setEmailVerifiedAt(user.getEmailVerifiedAt());
        entity.setPhoneVerifiedAt(user.getPhoneVerifiedAt());
        entity.setLockedUntil(user.getLockedUntil());

        // Update roles without replacing the collection reference
        entity.getRoles().clear();
        if (user.getRoles() != null) {
            entity.getRoles().addAll(user.getRoles());
        }

        UserEntity savedEntity = userRepository.save(entity);
        return identityUserMapper.toDomain(savedEntity);
    }
}
