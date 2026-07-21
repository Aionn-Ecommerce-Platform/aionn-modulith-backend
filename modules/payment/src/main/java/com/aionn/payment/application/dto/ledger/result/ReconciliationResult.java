package com.aionn.payment.application.dto.ledger.result;

import java.time.Instant;
import java.util.List;

public record ReconciliationResult(
        String reconciliationId,
        String gateway,
        int matchedCount,
        int mismatchedCount,
        List<String> mismatchedTransactionIds,
        Instant reconciledAt) {
}

