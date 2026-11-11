package com.aionn.identity.application.port.out.registration;

import com.aionn.identity.domain.model.RegistrationVerificationSession;

import java.util.Optional;

public interface RegistrationSessionStorePort {

    void save(RegistrationVerificationSession session);

    Optional<RegistrationVerificationSession> findByRegId(String regId);

    /**
     * Atomically reserves one verification attempt for the session. Returns {@code false} when
     * the session has already used up its attempt budget, so parallel guesses cannot bypass the
     * per-session limit by racing on the read-modify-write cycle.
     */
    boolean tryAcquireVerifyAttempt(String regId, int maxAttempts, java.time.Instant expiresAt);

    void resetVerifyAttempts(String regId);

    void deleteByRegId(String regId);
}
