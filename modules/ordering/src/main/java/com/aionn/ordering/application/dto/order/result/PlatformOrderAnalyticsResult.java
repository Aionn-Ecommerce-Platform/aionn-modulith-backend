package com.aionn.ordering.application.dto.order.result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PlatformOrderAnalyticsResult(
        LocalDate from,
        LocalDate to,
        String currency,
        BigDecimal totalGmv,
        long totalOrders,
        long completedOrders,
        List<MerchantOrderAnalyticsResult.RevenuePoint> revenueTrend,
        List<MerchantOrderAnalyticsResult.StatusCount> statusBreakdown,
        List<TopMerchant> topMerchants) {

    public record TopMerchant(
            String merchantId,
            BigDecimal revenue,
            long orders) {
    }
}
