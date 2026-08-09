package com.aionn.ordering.adapter.rest.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PlatformOrderAnalyticsResponse(
        LocalDate from, LocalDate to, String currency, BigDecimal totalGmv,
        long totalOrders, long completedOrders,
        List<MerchantOrderAnalyticsResponse.RevenuePoint> revenueTrend,
        List<MerchantOrderAnalyticsResponse.StatusCount> statusBreakdown,
        List<TopMerchant> topMerchants) {

    public record TopMerchant(String merchantId, BigDecimal revenue, long orders) {}
}
