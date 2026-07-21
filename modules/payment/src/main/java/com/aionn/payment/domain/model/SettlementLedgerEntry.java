package com.aionn.payment.domain.model;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
public class SettlementLedgerEntry {

    private final String entryId;
    private final String merchantId;
    private final String orderId;
    private final String paymentId;
    private final String payoutId;
    private final SettlementKind kind;
    private final BigDecimal gross;
    private final BigDecimal commission;
    private final BigDecimal net;
    private final String currency;
    private final String note;
    private final Instant createdAt;

    public SettlementLedgerEntry(String entryId, String merchantId, String orderId, String paymentId,
            String payoutId, SettlementKind kind, BigDecimal gross, BigDecimal commission, BigDecimal net,
            String currency, String note, Instant createdAt) {
        this.entryId = entryId;
        this.merchantId = merchantId;
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.payoutId = payoutId;
        this.kind = kind;
        this.gross = gross;
        this.commission = commission;
        this.net = net;
        this.currency = currency;
        this.note = note;
        this.createdAt = createdAt;
    }

    public enum SettlementKind {
        SALE,
        MOVE_AVAILABLE,
        REVERSAL,
        REFUND,
        PAYOUT_DEBIT,
        PAYOUT_REVERSAL
    }
}
