package com.aionn.recommendation.application.port.out;

import com.aionn.recommendation.domain.model.UserAffinityProfile;

import java.time.Instant;
import java.util.Optional;

public interface UserAffinityProfilePersistencePort {

    Optional<UserAffinityProfile> findByUserId(String userId);

    void save(UserAffinityProfile profile, Instant refreshedAt);

    int deleteByUserId(String userId);
}
