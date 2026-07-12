package com.aionn.identity.application.service;

import com.aionn.identity.application.dto.preference.command.UpdateAiPrivacyPreferenceCommand;
import com.aionn.identity.application.dto.preference.command.UpdateGeneralPreferenceCommand;
import com.aionn.identity.application.dto.preference.command.UpdateNotificationPreferenceCommand;
import com.aionn.identity.application.dto.preference.result.UserPreferenceResult;
import com.aionn.identity.application.mapper.UserPreferenceResultMapper;
import com.aionn.identity.application.port.out.preference.UserPreferencePersistencePort;
import com.aionn.identity.application.port.out.user.UserPersistencePort;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Clock;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PreferenceService {

    private final UserPersistencePort userPersistencePort;
    private final UserPreferencePersistencePort preferencePersistencePort;
    private final UserPreferenceResultMapper userPreferenceResultMapper;

    private final Clock clock;
    public UserPreferenceResult updateGeneral(UpdateGeneralPreferenceCommand command) {
        log.info("Updating general preferences for user: {}", command.userId());
        UserPreferenceResult preference = getOrCreate(command.userId());

        return preferencePersistencePort.save(
                userPreferenceResultMapper.applyGeneral(preference, command, nowUtc()));
    }

    public UserPreferenceResult updateNotifications(UpdateNotificationPreferenceCommand command) {
        log.info("Updating notification preferences for user: {}", command.userId());
        UserPreferenceResult preference = getOrCreate(command.userId());

        return preferencePersistencePort.save(
                userPreferenceResultMapper.applyNotifications(preference, command, nowUtc()));
    }

    public UserPreferenceResult updateAiPrivacy(UpdateAiPrivacyPreferenceCommand command) {
        log.info("Updating AI privacy preferences for user: {}", command.userId());
        UserPreferenceResult preference = getOrCreate(command.userId());

        return preferencePersistencePort.save(
                userPreferenceResultMapper.applyAiPrivacy(preference, command, nowUtc()));
    }

    public UserPreferenceResult get(String userId) {
        log.debug("Getting preferences for user: {}", userId);
        return getOrCreate(userId);
    }

    private UserPreferenceResult getOrCreate(String userId) {
        userPersistencePort.findById(userId)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.USER_NOT_FOUND));

        Optional<UserPreferenceResult> existing = preferencePersistencePort.findById(userId);
        if (existing.isPresent()) {
            return existing.get();
        }
        try {
            return preferencePersistencePort.createDefault(userId);
        } catch (DataIntegrityViolationException e) {
            log.warn("Preference already created concurrently for user: {}", userId, e);
            return preferencePersistencePort.findById(userId)
                    .orElseThrow(() -> new IdentityException(IdentityErrorCode.USER_NOT_FOUND));
        }
    }
    private Instant nowUtc() {
        return clock.instant();
    }
}
