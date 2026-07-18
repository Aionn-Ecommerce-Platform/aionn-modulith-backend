package com.aionn.ordering.adapter.rest.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderReturnResponse(
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
