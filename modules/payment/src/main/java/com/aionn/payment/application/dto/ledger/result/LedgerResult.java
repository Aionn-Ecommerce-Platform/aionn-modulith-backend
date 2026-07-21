package com.aionn.payment.application.dto.ledger.result;

import java.math.BigDecimal;
import java.time.Instant;

public record LedgerResult(
        String ledgerId,
        String paymentId,
        BigDecimal amount,
        String currency,
        String type,
        String gateway,
        String gatewayTransactionNo,
        Instant occurredAt) {
}

