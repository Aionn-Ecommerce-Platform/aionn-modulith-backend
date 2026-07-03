package com.aionn.identity.application.port.out.auth;

import com.aionn.identity.domain.model.AuthSession;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuthSessionPersistencePort {

    AuthSession save(AuthSession session);

    Optional<AuthSession> findById(String sessionId);

    List<AuthSession> findByUserId(String userId);

    List<AuthSession> saveAll(List<AuthSession> sessions);

    /**
     * Delete sessions whose lastActiveAt (or createdAt if null) is older than
     * the cutoff. Safe to call repeatedly. Returns the number of rows deleted.
     */
    int deleteIdleBefore(LocalDateTime cutoff);
}
