package com.aionn.ordering.application.dto.returns.result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReturnAnalyticsResult(
        LocalDate from,
        LocalDate to,
        long totalReturns,
        long totalCompletedOrders,
        double returnRate,
        BigDecimal totalRefundAmount,
        String currency,
        List<StatusCount> returnsByStatus,
        List<ReasonCount> returnsByReason) {

    public record StatusCount(String status, long count) {
    }

    public record ReasonCount(String reason, long count) {
    }
}
