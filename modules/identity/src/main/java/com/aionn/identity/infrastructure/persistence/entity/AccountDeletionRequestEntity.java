package com.aionn.identity.infrastructure.persistence.entity;

import com.aionn.identity.domain.valueobject.AccountDeletionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "account_deletion_requests", indexes = {
        @Index(name = "idx_account_deletion_user_id", columnList = "user_id"),
        @Index(name = "idx_account_deletion_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AccountDeletionRequestEntity {

    @Id
    @Column(name = "deletion_request_id", nullable = false, length = 26)
    private String deletionRequestId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AccountDeletionStatus status;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "scheduled_deletion_at", nullable = false)
    private LocalDateTime scheduledDeletionAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;
}


