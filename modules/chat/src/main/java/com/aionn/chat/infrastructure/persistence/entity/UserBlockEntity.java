package com.aionn.chat.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "chat_user_blocks", indexes = {
        @Index(name = "idx_user_blocks_blocker", columnList = "blocker_id"),
        @Index(name = "idx_user_blocks_blocker_blocked", columnList = "blocker_id, blocked_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBlockEntity {

    @Id
    @Column(name = "block_id", length = 50)
    private String blockId;

    @Column(name = "blocker_id", length = 50, nullable = false)
    private String blockerId;

    @Column(name = "blocked_id", length = 50, nullable = false)
    private String blockedId;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @jakarta.persistence.Version
    @Column(name = "version", nullable = false)
    private long version;
}
