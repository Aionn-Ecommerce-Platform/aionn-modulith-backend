package com.aionn.promotion.infrastructure.persistence.entity;

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

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "user_vouchers", indexes = {
        @Index(name = "idx_user_vouchers_user", columnList = "user_id"),
        @Index(name = "idx_user_vouchers_user_voucher", columnList = "user_id, voucher_code", unique = true),
        @Index(name = "idx_user_vouchers_status_expires", columnList = "status, reserved_expires_at"),
        @Index(name = "idx_user_vouchers_order", columnList = "reserved_order_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserVoucherEntity {

    @Id
    @Column(name = "user_voucher_id", length = 50)
    private String userVoucherId;

    @Column(name = "voucher_code", length = 50, nullable = false)
    private String voucherCode;

    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "reserved_order_id", length = 50)
    private String reservedOrderId;

    @Column(name = "applied_amount", precision = 18, scale = 2)
    private BigDecimal appliedAmount;

    @Column(name = "applied_currency", length = 3)
    private String appliedCurrency;

    @CreationTimestamp
    @Column(name = "claimed_at", updatable = false)
    private Instant claimedAt;

    @Column(name = "reserved_at")
    private Instant reservedAt;

    @Column(name = "reserved_expires_at")
    private Instant reservedExpiresAt;

    @Column(name = "applied_at")
    private Instant appliedAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @jakarta.persistence.Version
    @Column(name = "version", nullable = false)
    private long version;
}
