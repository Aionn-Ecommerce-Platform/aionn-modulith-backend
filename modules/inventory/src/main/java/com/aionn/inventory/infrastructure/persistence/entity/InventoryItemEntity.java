package com.aionn.inventory.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "inventory_items", indexes = {
        @Index(name = "idx_inventory_warehouse", columnList = "warehouse_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItemEntity {

    @EmbeddedId
    private InventoryItemId id;

    @Column(name = "physical_qty", nullable = false)
    private int physicalQty;

    @Column(name = "available_qty", nullable = false)
    private int availableQty;

    @Column(name = "safety_stock_qty", nullable = false)
    private int safetyStockQty;

    @Column(name = "is_locked", nullable = false)
    private boolean locked;

    @Column(name = "batch_no", length = 100)
    private String batchNo;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @EqualsAndHashCode
    public static class InventoryItemId implements Serializable {
        @Column(name = "sku_id", nullable = false, length = 50)
        private String skuId;
        @Column(name = "warehouse_id", nullable = false, length = 50)
        private String warehouseId;
    }
}
