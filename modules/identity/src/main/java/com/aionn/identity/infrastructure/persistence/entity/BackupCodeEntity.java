package com.aionn.identity.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "backup_codes", indexes = {
        @Index(name = "idx_backup_user_id", columnList = "user_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class BackupCodeEntity {

    @Id
    @Column(name = "backup_code_id", nullable = false, length = 26)
    private String backupCodeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "code_hash", nullable = false, unique = true, length = 255)
    private String codeHash;

    @CreationTimestamp
    @Column(name = "generated_at", nullable = false, updatable = false)
    private Instant generatedAt;

    @Column(name = "used_at")
    private Instant usedAt;
}



