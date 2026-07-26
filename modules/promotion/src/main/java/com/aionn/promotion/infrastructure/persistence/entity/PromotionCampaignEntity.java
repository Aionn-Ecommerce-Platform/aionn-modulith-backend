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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "promotion_campaigns", indexes = {
        @Index(name = "idx_campaigns_status_dates", columnList = "status, start_date, end_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionCampaignEntity {

    @Id
    @Column(name = "campaign_id", length = 50)
    private String campaignId;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "type", length = 20, nullable = false)
    private String type;

    @Column(name = "budget", precision = 18, scale = 2, nullable = false)
    private BigDecimal budget;

    @Column(name = "budget_remaining", precision = 18, scale = 2, nullable = false)
    private BigDecimal budgetRemaining;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    @Column(name = "end_date", nullable = false)
    private Instant endDate;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "min_order_value", precision = 18, scale = 2)
    private BigDecimal minOrderValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "applicable_categories", columnDefinition = "jsonb")
    private List<String> applicableCategories;

    @Column(name = "max_claims_per_user")
    private Integer maxClaimsPerUser;

    @Column(name = "max_uses_per_voucher")
    private Integer maxUsesPerVoucher;

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
