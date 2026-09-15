package com.aionn.recommendation.application.port.out;

import com.aionn.recommendation.domain.model.UserAffinityProfile;

import java.time.Instant;
import java.util.Optional;

public interface UserAffinityProfilePersistencePort {

    Optional<UserAffinityProfile> findByUserId(String userId);

    /**
     * Whether a profile row exists for this user, without materialising it.
     *
     * <p>Lets a refresh with no signal to work from distinguish "this user's history aged out, clear the
     * stored profile" from "we hold nothing on this user". The second case must not create a row: an
     * empty profile is still personal data about an identifiable person, and there is nothing to correct.
     */
    boolean existsByUserId(String userId);

    void save(UserAffinityProfile profile, Instant refreshedAt);

    int deleteByUserId(String userId);
}
