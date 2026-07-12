package com.aionn.identity.domain.model;

import com.aionn.identity.domain.valueobject.AuthSessionStatus;
import lombok.Getter;

import java.time.Clock;
import java.time.Instant;

@Getter
public class AuthSession {

    private final String sessionId;
    private final String userId;
    private final String ipAddress;
    private final String userAgent;
    private AuthSessionStatus status;
    private final Instant createdAt;
    private Instant lastActiveAt;
    private Instant expiresAt;

    public AuthSession(
            String sessionId,
            String userId,
            String ipAddress,
            String userAgent,
            AuthSessionStatus status,
            Instant createdAt,
            Instant lastActiveAt,
            Instant expiresAt) {
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
            Instant expiresAt) {
        return createNew(sessionId, userId, ipAddress, userAgent, expiresAt, Clock.systemUTC());
    }

    public static AuthSession createNew(
            String sessionId,
            String userId,
            String ipAddress,
            String userAgent,
            Instant expiresAt,
            Clock clock) {
        Instant now = clock.instant();
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
        touch(Clock.systemUTC());
    }

    public void touch(Clock clock) {
        this.lastActiveAt = clock.instant();
    }

    public void revoke() {
        this.status = AuthSessionStatus.REVOKED;
    }

    public void extendExpiry(Instant newExpiresAt) {
        extendExpiry(newExpiresAt, Clock.systemUTC());
    }

    public void extendExpiry(Instant newExpiresAt, Clock clock) {
        if (newExpiresAt == null || !newExpiresAt.isAfter(clock.instant())) {
            throw new IllegalArgumentException("New expiry must be in the future");
        }
        this.expiresAt = newExpiresAt;
        this.lastActiveAt = clock.instant();
    }

    public boolean isActive() {
        return isActive(Clock.systemUTC());
    }

    public boolean isActive(Clock clock) {
        return AuthSessionStatus.ACTIVE.equals(status)
                && expiresAt != null
                && expiresAt.isAfter(clock.instant());
    }

    public boolean isExpired() {
        return isExpired(Clock.systemUTC());
    }

    public boolean isExpired(Clock clock) {
        return expiresAt != null && !expiresAt.isAfter(clock.instant());
    }
}
