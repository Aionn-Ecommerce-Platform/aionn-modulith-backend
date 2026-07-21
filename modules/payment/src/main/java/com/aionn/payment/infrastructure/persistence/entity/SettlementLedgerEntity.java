package com.aionn.payment.infrastructure.persistence.entity;

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

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "settlement_ledger", indexes = {
        @Index(name = "idx_settlement_ledger_merchant", columnList = "merchant_id,created_at"),
        @Index(name = "idx_settlement_ledger_order", columnList = "order_id"),
        @Index(name = "idx_settlement_ledger_payout", columnList = "payout_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementLedgerEntity {

    @Id
    @Column(name = "entry_id", length = 50)
    private String entryId;

    @Column(name = "merchant_id", length = 50, nullable = false)
    private String merchantId;

    @Column(name = "order_id", length = 50)
    private String orderId;

    @Column(name = "payment_id", length = 50)
    private String paymentId;

    @Column(name = "payout_id", length = 50)
    private String payoutId;

    @Column(name = "kind", length = 20, nullable = false)
    private String kind;

    @Column(name = "gross", nullable = false, precision = 18, scale = 2)
    private BigDecimal gross;

    @Column(name = "commission", nullable = false, precision = 18, scale = 2)
    private BigDecimal commission;

    @Column(name = "net", nullable = false, precision = 18, scale = 2)
    private BigDecimal net;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
