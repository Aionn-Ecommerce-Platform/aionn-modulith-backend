package com.aionn.recommendation.infrastructure.persistence.entity;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "recommendation_interactions", indexes = {
        @Index(name = "idx_rec_interactions_user_time", columnList = "user_id, occurred_at"),
        @Index(name = "idx_rec_interactions_product_time", columnList = "product_id, occurred_at"),
        @Index(name = "idx_rec_interactions_type_user", columnList = "interaction_type, user_id"),
        @Index(name = "idx_rec_interactions_occurred_at", columnList = "occurred_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InteractionEntity {

    @Id
    @Column(name = "interaction_id", nullable = false, length = 50)
    private String interactionId;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    // Nullable for SEARCH interactions, which capture filter intent rather than a
    // concrete product.
    @Column(name = "product_id", length = 50)
    private String productId;

    @Column(name = "interaction_type", nullable = false, length = 20)
    private String interactionType;

    @Column(name = "weight", nullable = false, precision = 5, scale = 2)
    private BigDecimal weight;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Integration event that produced this row, for idempotent ingest under at-least-once delivery.
     * Nullable: seeded fixtures and backfills have no originating event.
     */
    @Column(name = "source_event_id", length = 100)
    private String sourceEventId;

    /** Search-intent facets, only set for SEARCH interactions. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "category_ids", columnDefinition = "jsonb")
    private List<String> categoryIds;

    @Column(name = "brand_id", length = 50)
    private String brandId;

    @Column(name = "keyword", length = 255)
    private String keyword;

    /** JSON form for the native INSERT; JPA reads {@link #categoryIds} directly. */
    public String getCategoryIdsJson() {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < categoryIds.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"').append(categoryIds.get(i).replace("\"", "\\\"")).append('"');
        }
        return sb.append(']').toString();
    }
}
