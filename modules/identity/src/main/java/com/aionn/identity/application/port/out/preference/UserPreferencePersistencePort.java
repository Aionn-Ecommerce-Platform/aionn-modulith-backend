package com.aionn.identity.application.port.out.preference;

import com.aionn.identity.application.dto.preference.result.UserPreferenceResult;

import java.util.Optional;

public interface UserPreferencePersistencePort {
    UserPreferenceResult save(UserPreferenceResult preference);

    Optional<UserPreferenceResult> findById(String userId);

    UserPreferenceResult createDefault(String userId);
}
