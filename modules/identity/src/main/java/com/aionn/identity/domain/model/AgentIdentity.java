package com.aionn.identity.domain.model;

import com.aionn.identity.domain.valueobject.AgentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Clock;
import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class AgentIdentity {

    private final String id;
    private final String ownerId;
    private final String name;
    private final String keyHash;
    private String permissions;
    private AgentStatus status;
    private final Instant expiresAt;
    private final Instant createdAt;
    private Instant updatedAt;

    public void updatePermissions(String permissions) {
        updatePermissions(permissions, Clock.systemUTC());
    }

    public void updatePermissions(String permissions, Clock clock) {
        this.permissions = permissions;
        this.updatedAt = clock.instant();
    }

    public void suspend() {
        suspend(Clock.systemUTC());
    }

    public void suspend(Clock clock) {
        this.status = AgentStatus.SUSPENDED;
        this.updatedAt = clock.instant();
    }

    public void revoke() {
        revoke(Clock.systemUTC());
    }

    public void revoke(Clock clock) {
        this.status = AgentStatus.REVOKED;
        this.updatedAt = clock.instant();
    }

    public void activate() {
        activate(Clock.systemUTC());
    }

    public void activate(Clock clock) {
        this.status = AgentStatus.ACTIVE;
        this.updatedAt = clock.instant();
    }

    public boolean isActive() {
        return status == AgentStatus.ACTIVE;
    }

    public boolean isExpired() {
        return isExpired(Clock.systemUTC());
    }

    public boolean isExpired(Clock clock) {
        return expiresAt != null && !expiresAt.isAfter(clock.instant());
    }
}
