package com.aionn.catalog.infrastructure.persistence.entity;

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
@Table(name = "merchants", indexes = {
        @Index(name = "idx_merchants_owner", columnList = "owner_id"),
        @Index(name = "idx_merchants_status", columnList = "status"),
        @Index(name = "idx_merchants_province", columnList = "province_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantEntity {

    @Id
    @Column(name = "merchant_id", nullable = false, length = 50)
    private String merchantId;

    @Column(name = "owner_id", nullable = false, length = 50)
    private String ownerId;

    @Column(name = "name", length = 150)
    private String name;

    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "province_code", length = 10)
    private String provinceCode;

    @Column(name = "province_name", length = 100)
    private String provinceName;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 4)
    private java.math.BigDecimal commissionRate;

    @Column(name = "stripe_account_id", length = 100)
    private String stripeAccountId;

    @Column(name = "stripe_charges_enabled", nullable = false)
    private boolean stripeChargesEnabled;

    @Column(name = "stripe_payouts_enabled", nullable = false)
    private boolean stripePayoutsEnabled;

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
