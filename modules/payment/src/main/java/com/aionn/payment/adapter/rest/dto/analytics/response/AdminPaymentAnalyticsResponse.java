package com.aionn.payment.adapter.rest.dto.analytics.response;

import com.aionn.payment.application.dto.analytics.result.AdminPaymentAnalyticsResult.DailyAmount;
import com.aionn.payment.application.dto.analytics.result.AdminPaymentAnalyticsResult.StatusCount;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AdminPaymentAnalyticsResponse(
        LocalDate from, LocalDate to, String currency,
        BigDecimal totalRefundAmount, long totalRefundCount, long totalPaidCount, double refundRate,
        BigDecimal totalPayoutAmount, long totalPayoutCount,
        List<DailyAmount> payoutVolumeTrend,
        List<StatusCount> paymentStatusBreakdown,
        List<StatusCount> payoutStatusBreakdown) {
}
