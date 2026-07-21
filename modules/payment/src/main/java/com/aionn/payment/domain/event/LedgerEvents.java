package com.aionn.payment.domain.event;

import com.aionn.payment.domain.valueobject.LedgerEntryType;

import java.math.BigDecimal;
import java.time.Instant;

public final class LedgerEvents {

    private LedgerEvents() {
    }

    public record LedgerEntryRecorded(
            String ledgerId,
            String paymentId,
            BigDecimal amount,
            String currency,
            LedgerEntryType type,
            String gateway,
            Instant occurredAt) implements PaymentEvent {
    }

    public record PaymentReconciled(
            String reconciliationId,
            String gateway,
            int matchedCount,
            int mismatchedCount,
            Instant reconciledAt,
            Instant occurredAt) implements PaymentEvent {
    }
}

