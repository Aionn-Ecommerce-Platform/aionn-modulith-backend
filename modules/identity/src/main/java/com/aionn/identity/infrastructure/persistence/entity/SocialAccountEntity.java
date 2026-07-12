package com.aionn.identity.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "social_accounts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_social_provider_id", columnNames = {"provider", "provider_user_id"})
}, indexes = {
        @Index(name = "idx_social_user_id", columnList = "user_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SocialAccountEntity {

    @Id
    @Column(name = "social_account_id", nullable = false, length = 26)
    private String socialAccountId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "provider", length = 20, nullable = false)
    private String provider;

    @Column(name = "provider_user_id", length = 100, nullable = false)
    private String providerUserId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

}



