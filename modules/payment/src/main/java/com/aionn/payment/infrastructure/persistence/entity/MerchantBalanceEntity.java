package com.aionn.payment.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.IdClass;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "merchant_balances", indexes = {
        @Index(name = "idx_merchant_balances_merchant", columnList = "merchant_id")
})
@IdClass(MerchantBalanceEntity.MerchantBalanceId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantBalanceEntity {

    @Id
    @Column(name = "merchant_id", length = 50)
    private String merchantId;

    @Id
    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "pending", nullable = false, precision = 18, scale = 2)
    private BigDecimal pending;

    @Column(name = "available", nullable = false, precision = 18, scale = 2)
    private BigDecimal available;

    @jakarta.persistence.Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @EqualsAndHashCode
    public static class MerchantBalanceId implements Serializable {
        private String merchantId;
        private String currency;

        public MerchantBalanceId() {}

        public MerchantBalanceId(String merchantId, String currency) {
            this.merchantId = merchantId;
            this.currency = currency;
        }
    }
}
