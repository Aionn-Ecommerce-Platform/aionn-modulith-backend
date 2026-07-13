package com.aionn.inventory.infrastructure.persistence.entity;

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

import java.time.Instant;

@Entity
@Table(name = "stock_adjustments", indexes = {
        @Index(name = "idx_stock_adjustments_sku_warehouse", columnList = "sku_id, warehouse_id"),
        @Index(name = "idx_stock_adjustments_order", columnList = "order_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockAdjustmentEntity {

    @Id
    @Column(name = "adj_id", length = 50, nullable = false)
    private String adjId;

    @Column(name = "sku_id", length = 50, nullable = false)
    private String skuId;

    @Column(name = "warehouse_id", length = 50, nullable = false)
    private String warehouseId;

    @Column(name = "qty", nullable = false)
    private int qty;

    @Column(name = "type", length = 30, nullable = false)
    private String type;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "order_id", length = 50)
    private String orderId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}

