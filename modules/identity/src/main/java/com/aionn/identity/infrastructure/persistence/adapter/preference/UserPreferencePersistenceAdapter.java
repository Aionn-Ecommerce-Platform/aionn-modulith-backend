package com.aionn.identity.infrastructure.persistence.adapter.preference;

import com.aionn.identity.application.dto.preference.result.UserPreferenceResult;
import com.aionn.identity.application.port.out.preference.UserPreferencePersistencePort;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.infrastructure.persistence.entity.UserPreferenceEntity;
import com.aionn.identity.infrastructure.persistence.mapper.UserPreferenceDomainMapper;
import com.aionn.identity.infrastructure.persistence.repository.preference.UserPreferenceRepository;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserPreferencePersistenceAdapter implements UserPreferencePersistencePort {

    private final UserPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;
    private final UserPreferenceDomainMapper mapper;

    @Override
    public UserPreferenceResult save(UserPreferenceResult preference) {
        UserPreferenceEntity entity = preferenceRepository.findById(preference.userId())
                .orElseGet(() -> {
                    var user = userRepository.findById(preference.userId())
                            .orElseThrow(() -> new IdentityException(IdentityErrorCode.USER_NOT_FOUND));
                    return UserPreferenceEntity.builder()
                            .userId(preference.userId())
                            .user(user)
                            .build();
                });

        entity.setLanguage(preference.language());
        entity.setCurrency(preference.currency());
        entity.setTimezone(preference.timezone());
        entity.setTheme(preference.theme());
        entity.setNotificationSettings(preference.notificationSettings());
        entity.setAiPrivacySettings(preference.aiPrivacySettings());
        entity.setUpdatedAt(preference.updatedAt());

        UserPreferenceEntity saved = preferenceRepository.save(entity);
        return mapper.toResult(saved);
    }

    @Override
    public Optional<UserPreferenceResult> findById(String userId) {
        return preferenceRepository.findById(userId)
                .map(mapper::toResult);
    }

    @Override
    public Optional<UserPreferenceResult> findByIdForUpdate(String userId) {
        return preferenceRepository.findByIdForUpdate(userId)
                .map(mapper::toResult);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public UserPreferenceResult createDefault(String userId) {
        // Serialize creation on the user row, then re-check: two concurrent first-time
        // requests must not both insert.
        var user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.USER_NOT_FOUND));

        var existing = preferenceRepository.findById(userId);
        if (existing.isPresent()) {
            return mapper.toResult(existing.get());
        }

        UserPreferenceEntity entity = UserPreferenceEntity.builder()
                .userId(userId)
                .user(user)
                .build();

        UserPreferenceEntity saved = preferenceRepository.save(entity);
        return mapper.toResult(saved);
    }
}
