package com.aionn.identity.application.port.out.security;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetPort {

    void savePasswordResetTokenHash(String tokenHash, String userId, LocalDateTime expiresAt);

    Optional<PasswordResetTokenData> findPasswordResetTokenHash(String tokenHash);

    Optional<PasswordResetTokenData> consumePasswordResetTokenHash(String tokenHash);

    void deletePasswordResetTokenHash(String tokenHash);

    void updatePassword(String userId, String passwordHash);

    record PasswordResetTokenData(String userId, LocalDateTime expiresAt) {
    }
}
