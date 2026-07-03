package com.aionn.identity.application.port.out.registration;

import java.util.Optional;

/**
 * Distributed lock primitive used during registration to serialise concurrent
 * "complete registration" attempts on the same phone number.
 */
public interface RegistrationLockManagerPort {

    /**
     * Attempts to acquire a lock for {@code key} with the given timeout.
     *
     * @return the lock token when acquired, otherwise {@link Optional#empty()}
     */
    Optional<String> tryLock(String key, int timeoutSeconds);

    /**
     * Releases the lock identified by {@code lockToken} on {@code key}
     * (best-effort).
     */
    void unlock(String key, String lockToken);

    /**
     * Schedules the lock release to run after the surrounding transaction
     * completes (success or rollback). When called outside an active
     * transaction, the unlock happens immediately.
     */
    void unlockAfterCompletion(String key, String lockToken);
}
