package com.aionn.promotion.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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
@Table(name = "flash_sale_registrations", indexes = {
        @Index(name = "idx_flash_sale_status_campaign", columnList = "status, campaign_id"),
        @Index(name = "idx_flash_sale_merchant", columnList = "merchant_id, status"),
        @Index(name = "uq_flash_sale_campaign_sku", columnList = "campaign_id, sku_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlashSaleRegistrationEntity {

    @Id
    @Column(name = "registration_id", length = 50)
    private String registrationId;

    @Column(name = "campaign_id", length = 50, nullable = false)
    private String campaignId;

    @Column(name = "merchant_id", length = 50, nullable = false)
    private String merchantId;

    @Column(name = "product_id", length = 50, nullable = false)
    private String productId;

    @Column(name = "sku_id", length = 50, nullable = false)
    private String skuId;

    @Column(name = "sale_price", precision = 18, scale = 2, nullable = false)
    private BigDecimal salePrice;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Column(name = "sale_stock", nullable = false)
    private int saleStock;

    @Column(name = "sold_count", nullable = false)
    private int soldCount;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    @CreationTimestamp
    @Column(name = "submitted_at", updatable = false)
    private Instant submittedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decided_by", length = 50)
    private String decidedBy;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
