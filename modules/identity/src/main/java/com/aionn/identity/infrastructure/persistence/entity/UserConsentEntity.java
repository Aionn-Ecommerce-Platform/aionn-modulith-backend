package com.aionn.identity.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_consents", indexes = {
        @Index(name = "idx_user_consents_user_id", columnList = "user_id"),
        @Index(name = "idx_user_consents_type", columnList = "consent_type")
})
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserConsentEntity {

    @Id
    @Column(name = "consent_id", nullable = false, length = 26)
    private String consentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "consent_type", length = 50, nullable = false)
    private String consentType;

    @Column(name = "version", length = 20, nullable = false)
    private String version;

    @CreationTimestamp
    @Column(name = "agreed_at", updatable = false)
    private LocalDateTime agreedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;
}



