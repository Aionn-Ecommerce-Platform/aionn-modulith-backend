package com.aionn.identity.application.port.out.registration;

/**
 * Distributed lock primitive used during registration to serialise concurrent
 * "complete registration" attempts on the same phone number.
 */
public interface RegistrationLockManagerPort {

    /**
     * Attempts to acquire a lock for {@code key} with the given timeout.
     *
     * @return a non-empty lock token if acquired, an empty string otherwise
     */
    String tryLock(String key, int timeoutSeconds);

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
