package com.aionn.ordering.application.dto.returns.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ReturnResult(
        String returnId,
        String orderId,
        String userId,
        String merchantId,
        String reasonType,
        String reason,
        String evidenceUrl,
        List<String> evidenceUrls,
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

