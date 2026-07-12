package com.aionn.identity.application.port.out.security;

import com.aionn.identity.domain.valueobject.UserStatus;

import java.time.Instant;
import java.util.Optional;

public interface UserSecurityPort {

    Optional<UserSecurityData> findById(String userId);

    Optional<UserSecurityData> findByIdentity(String identity);

    void recordFailedLoginAttempt(String userId, int failedAttempts, Instant lockedUntil);

    void resetFailedLoginAttempts(String userId);

    record UserSecurityData(
            String userId,
            String passwordHash,
            UserStatus status,
            boolean mfaEnabled,
            String mfaSecret,
            Instant lockedUntil,
            int failedLoginAttempts) {
        @Override
        public String toString() {
            return "UserSecurityData[userId=%s, credentialHash=***, status=%s, mfaEnabled=%s, mfaSecret=***, lockedUntil=%s, failedLoginAttempts=%s]"
                    .formatted(userId, status, mfaEnabled, lockedUntil, failedLoginAttempts);
        }
    }
}
