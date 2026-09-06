package com.aionn.recommendation.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
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
@Table(name = "recommendation_item_similarity")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemSimilarityEntity {

    @EmbeddedId
    private ItemSimilarityId id;

    @Column(name = "score", nullable = false, precision = 6, scale = 5)
    private BigDecimal score;

    @Column(name = "co_occurrence", nullable = false)
    private int coOccurrence;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @EqualsAndHashCode
    public static class ItemSimilarityId implements Serializable {

        @Column(name = "product_id", nullable = false, length = 50)
        private String productId;

        @Column(name = "similar_product_id", nullable = false, length = 50)
        private String similarProductId;
    }
}
