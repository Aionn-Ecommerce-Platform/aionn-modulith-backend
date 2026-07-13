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
@Table(name = "stock_reservations", indexes = {
        @Index(name = "idx_stock_reservations_order", columnList = "order_id"),
        @Index(name = "idx_stock_reservations_status_expires", columnList = "status, expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReservationEntity {

    @Id
    @Column(name = "reservation_id", length = 50, nullable = false)
    private String reservationId;

    @Column(name = "sku_id", length = 50, nullable = false)
    private String skuId;

    @Column(name = "warehouse_id", length = 50, nullable = false)
    private String warehouseId;

    @Column(name = "order_id", length = 50)
    private String orderId;

    @Column(name = "qty", nullable = false)
    private int qty;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "reserved_at", nullable = false)
    private Instant reservedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @jakarta.persistence.Version
    @Column(name = "version", nullable = false)
    private long version;
}
