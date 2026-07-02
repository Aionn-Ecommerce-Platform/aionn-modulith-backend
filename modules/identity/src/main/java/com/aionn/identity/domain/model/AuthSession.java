package com.aionn.identity.domain.model;

import com.aionn.identity.domain.valueobject.AuthSessionStatus;
import lombok.Getter;

import java.time.Clock;
import java.time.LocalDateTime;

@Getter
public class AuthSession {

    private final String sessionId;
    private final String userId;
    private final String ipAddress;
    private final String userAgent;
    private AuthSessionStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime lastActiveAt;
    private LocalDateTime expiresAt;

    @SuppressWarnings("java:S107")
    public AuthSession(
            String sessionId,
            String userId,
            String ipAddress,
            String userAgent,
            AuthSessionStatus status,
            LocalDateTime createdAt,
            LocalDateTime lastActiveAt,
            LocalDateTime expiresAt) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.status = status;
        this.createdAt = createdAt;
        this.lastActiveAt = lastActiveAt;
        this.expiresAt = expiresAt;
    }

    public static AuthSession createNew(
            String sessionId,
            String userId,
            String ipAddress,
            String userAgent,
            LocalDateTime expiresAt) {
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        return new AuthSession(
                sessionId,
                userId,
                ipAddress,
                userAgent,
                AuthSessionStatus.ACTIVE,
                now,
                now,
                expiresAt);
    }

    public void touch() {
        this.lastActiveAt = LocalDateTime.now(Clock.systemUTC());
    }

    public void revoke() {
        this.status = AuthSessionStatus.REVOKED;
    }

    public void extendExpiry(LocalDateTime newExpiresAt) {
        if (newExpiresAt == null || !newExpiresAt.isAfter(LocalDateTime.now(Clock.systemUTC()))) {
            throw new IllegalArgumentException("New expiry must be in the future");
        }
        this.expiresAt = newExpiresAt;
        this.lastActiveAt = LocalDateTime.now(Clock.systemUTC());
    }

    public boolean isActive() {
        return AuthSessionStatus.ACTIVE.equals(status)
                && expiresAt != null
                && expiresAt.isAfter(LocalDateTime.now(Clock.systemUTC()));
    }

    public boolean isExpired() {
        return expiresAt != null && !expiresAt.isAfter(LocalDateTime.now(Clock.systemUTC()));
    }
}
