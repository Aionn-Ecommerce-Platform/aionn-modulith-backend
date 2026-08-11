package com.aionn.payment.application.dto.analytics.result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AdminPaymentAnalyticsResult(
        LocalDate from, LocalDate to, String currency,
        BigDecimal totalRefundAmount, long totalRefundCount, long totalPaidCount, double refundRate,
        BigDecimal totalPayoutAmount, long totalPayoutCount,
        List<DailyAmount> payoutVolumeTrend,
        List<StatusCount> paymentStatusBreakdown,
        List<StatusCount> payoutStatusBreakdown) {
    public record DailyAmount(LocalDate date, BigDecimal amount, long count) {
    }

    public record StatusCount(String status, long count) {
    }
}
