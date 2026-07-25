package com.aionn.shipping.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
@Table(name = "shipping_rates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingRateEntity {

    @Id
    @Column(name = "rate_id", length = 50)
    private String rateId;

    @Column(name = "zone_code", length = 50, nullable = false, unique = true)
    private String zoneCode;

    @Column(name = "base_fee", precision = 18, scale = 2, nullable = false)
    private BigDecimal baseFee;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Column(name = "condition", columnDefinition = "TEXT")
    private String condition;

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
