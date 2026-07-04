package com.aionn.identity.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "security_audits", indexes = {
        @Index(name = "idx_audit_user_id", columnList = "user_id"),
        @Index(name = "idx_audit_event_type", columnList = "event_type"),
        @Index(name = "idx_audit_device_id", columnList = "device_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SecurityAuditEntity {

    @Id
    @Column(name = "audit_id", length = 26)
    private String auditId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "event_type", length = 100)
    private String eventType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    @CreationTimestamp
    @Column(name = "timestamp", updatable = false, nullable = false)
    private LocalDateTime timestamp;
}


