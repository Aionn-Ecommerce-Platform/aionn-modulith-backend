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

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "recommendation_popularity")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductPopularityEntity {

    @Id
    @Column(name = "product_id", nullable = false, length = 50)
    private String productId;

    @Column(name = "popularity_score", nullable = false, precision = 12, scale = 5)
    private BigDecimal popularityScore;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @Column(name = "purchase_count", nullable = false)
    private long purchaseCount;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;
}
