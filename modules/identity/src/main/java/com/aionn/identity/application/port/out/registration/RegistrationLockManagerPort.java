package com.aionn.identity.application.port.out.registration;

import java.util.Optional;

public interface RegistrationLockManagerPort {

    Optional<String> tryLock(String key, int timeoutSeconds);

    void unlock(String key, String lockToken);

    void unlockAfterCompletion(String key, String lockToken);
}
