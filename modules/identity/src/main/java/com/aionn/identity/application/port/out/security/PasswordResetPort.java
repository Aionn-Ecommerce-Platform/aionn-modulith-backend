package com.aionn.identity.application.port.out.security;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetPort {

    void savePasswordResetTokenHash(String tokenHash, String userId, Instant expiresAt);

    Optional<PasswordResetTokenData> consumePasswordResetTokenHash(String tokenHash);

    void deletePasswordResetTokensForUser(String userId);

    void updatePassword(String userId, String passwordHash);

    record PasswordResetTokenData(String userId, Instant expiresAt) {
    }
}
