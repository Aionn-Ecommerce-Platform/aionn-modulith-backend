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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "product_sold_counters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSoldCounterEntity {

    @Id
    @Column(name = "product_id", length = 50)
    private String productId;

    @Column(name = "sold_count", nullable = false)
    private long soldCount;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
