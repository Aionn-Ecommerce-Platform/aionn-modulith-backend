package com.aionn.identity.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_identities", indexes = {
        @Index(name = "idx_agent_owner_id", columnList = "owner_id"),
        @Index(name = "idx_agent_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AgentIdentityEntity {

    @Id
    @Column(name = "agent_id", nullable = false, length = 26)
    private String agentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @Column(name = "key_hash", nullable = false, length = 255)
    private String keyHash;

    @Column(name = "permissions", columnDefinition = "jsonb")
    private String permissions;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "expiry_at")
    private LocalDateTime expiryAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}



