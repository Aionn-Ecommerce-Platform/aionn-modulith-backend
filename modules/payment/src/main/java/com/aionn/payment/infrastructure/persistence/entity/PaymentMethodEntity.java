package com.aionn.payment.infrastructure.persistence.entity;

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
@Table(name = "payment_methods", indexes = {
        @Index(name = "idx_payment_methods_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMethodEntity {

    @Id
    @Column(name = "method_id", length = 50)
    private String methodId;

    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    @Column(name = "provider", length = 50, nullable = false)
    private String provider;

    @Column(name = "last_4_digits", length = 4)
    private String last4Digits;

    @Column(name = "gateway_token", length = 255, nullable = false)
    private String gatewayToken;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @jakarta.persistence.Version
    @Column(name = "version", nullable = false)
    private long version;
}
