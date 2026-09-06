package com.aionn.recommendation.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(name = "recommendation_user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAffinityProfileEntity {

    @Id
    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "category_affinities", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, BigDecimal> categoryAffinities = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "brand_affinities", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, BigDecimal> brandAffinities = new LinkedHashMap<>();

    @Column(name = "price_band_min", precision = 19, scale = 2)
    private BigDecimal priceBandMin;

    @Column(name = "price_band_max", precision = 19, scale = 2)
    private BigDecimal priceBandMax;

    @Column(name = "interaction_count", nullable = false)
    private int interactionCount;

    @Column(name = "last_interaction_at")
    private Instant lastInteractionAt;

    @Column(name = "refreshed_at", nullable = false)
    private Instant refreshedAt;
}
