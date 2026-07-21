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
@Table(name = "transaction_ledgers", indexes = {
        @Index(name = "idx_ledgers_payment", columnList = "payment_id"),
        @Index(name = "idx_ledgers_gateway_time", columnList = "gateway, occurred_at"),
        @Index(name = "idx_ledgers_gateway_txn", columnList = "gateway_transaction_no")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionLedgerEntity {

    @Id
    @Column(name = "ledger_id", length = 50)
    private String ledgerId;

    @Column(name = "payment_id", length = 50, nullable = false)
    private String paymentId;

    @Column(name = "amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Column(name = "type", length = 10, nullable = false)
    private String type;

    @Column(name = "gateway", length = 20, nullable = false)
    private String gateway;

    @Column(name = "gateway_transaction_no", length = 100)
    private String gatewayTransactionNo;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}

