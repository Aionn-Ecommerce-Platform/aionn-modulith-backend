package com.aionn.ordering.application.dto.returns.result;

import java.math.BigDecimal;
import java.time.Instant;

public record ReturnResult(
        String returnId,
        String orderId,
        String userId,
        String merchantId,
        String reason,
        String evidenceUrl,
        BigDecimal refundAmount,
        String currency,
        String returnWarehouseId,
        String itemCondition,
        String rejectionReason,
        String status,
        Instant requestedAt,
        Instant decidedAt,
        Instant receivedAt) {
}

