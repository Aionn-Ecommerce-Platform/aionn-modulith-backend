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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_browsing_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBrowsingHistoryEntity {

    @Id
    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "category_ids", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> categoryIds = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "brand_ids", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> brandIds = new ArrayList<>();

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
