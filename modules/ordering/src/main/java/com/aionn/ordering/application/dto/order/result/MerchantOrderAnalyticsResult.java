package com.aionn.ordering.application.dto.order.result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MerchantOrderAnalyticsResult(
        LocalDate from,
        LocalDate to,
        String currency,
        BigDecimal totalRevenue,
        long totalOrders,
        long completedOrders,
        List<RevenuePoint> revenueTrend,
        List<StatusCount> statusBreakdown) {

    public record RevenuePoint(
            LocalDate date,
            BigDecimal revenue,
            long orders) {
    }

    public record StatusCount(
            String status,
            long count) {
    }
}
