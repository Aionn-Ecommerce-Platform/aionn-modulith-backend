package com.aionn.catalog.infrastructure.persistence.entity;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product_reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductReviewEntity {

    @Id
    @Column(name = "review_id", nullable = false, length = 50)
    private String reviewId;

    @Column(name = "product_id", nullable = false, length = 50)
    private String productId;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "order_id", length = 50)
    private String orderId;

    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "image_urls", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "merchant_reply", columnDefinition = "TEXT")
    private String merchantReply;

    @Column(name = "merchant_replied_at")
    private Instant merchantRepliedAt;

    @Column(name = "reported_by_merchant_id", length = 50)
    private String reportedByMerchantId;

    @Column(name = "report_reason", columnDefinition = "TEXT")
    private String reportReason;

    @Column(name = "reported_at")
    private Instant reportedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
