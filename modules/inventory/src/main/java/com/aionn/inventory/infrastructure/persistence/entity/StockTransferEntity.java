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
@Table(name = "stock_transfers", indexes = {
        @Index(name = "idx_stock_transfers_merchant", columnList = "merchant_id"),
        @Index(name = "idx_stock_transfers_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransferEntity {

    @Id
    @Column(name = "transfer_id", length = 50, nullable = false)
    private String transferId;

    @Column(name = "merchant_id", length = 50, nullable = false)
    private String merchantId;

    @Column(name = "from_warehouse_id", length = 50, nullable = false)
    private String fromWarehouseId;

    @Column(name = "to_warehouse_id", length = 50, nullable = false)
    private String toWarehouseId;

    @Column(name = "sku_id", length = 50, nullable = false)
    private String skuId;

    @Column(name = "qty", nullable = false)
    private int qty;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "initiated_at", nullable = false)
    private Instant initiatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @jakarta.persistence.Version
    @Column(name = "version", nullable = false)
    private long version;
}
