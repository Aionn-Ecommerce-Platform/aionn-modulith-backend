package com.aionn.identity.application.port.out.security;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetPort {

    void savePasswordResetToken(String token, String userId, LocalDateTime expiresAt);

    Optional<PasswordResetTokenData> findPasswordResetToken(String token);

    Optional<PasswordResetTokenData> consumePasswordResetToken(String token);

    void deletePasswordResetToken(String token);

    void updatePassword(String userId, String passwordHash);

    record PasswordResetTokenData(String userId, LocalDateTime expiresAt) {
    }
}
