package com.aionn.identity.domain.model;

import com.aionn.identity.domain.valueobject.AgentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Clock;
import java.time.LocalDateTime;

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
    private final LocalDateTime expiresAt;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void updatePermissions(String permissions) {
        this.permissions = permissions;
        this.updatedAt = LocalDateTime.now(Clock.systemUTC());
    }

    public void suspend() {
        this.status = AgentStatus.SUSPENDED;
        this.updatedAt = LocalDateTime.now(Clock.systemUTC());
    }

    public void revoke() {
        this.status = AgentStatus.REVOKED;
        this.updatedAt = LocalDateTime.now(Clock.systemUTC());
    }

    public void activate() {
        this.status = AgentStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now(Clock.systemUTC());
    }

    public boolean isActive() {
        return status == AgentStatus.ACTIVE;
    }

    public boolean isExpired() {
        return expiresAt != null && !expiresAt.isAfter(LocalDateTime.now(Clock.systemUTC()));
    }
}
