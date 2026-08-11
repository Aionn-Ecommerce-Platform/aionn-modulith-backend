package com.aionn.payment.infrastructure.persistence.adapter.analytics;

import com.aionn.payment.application.dto.analytics.result.AdminPaymentAnalyticsResult;
import com.aionn.payment.application.dto.analytics.result.AdminPaymentAnalyticsResult.DailyAmount;
import com.aionn.payment.application.dto.analytics.result.AdminPaymentAnalyticsResult.StatusCount;
import com.aionn.payment.application.port.out.PaymentAnalyticsQueryPort;
import com.aionn.payment.infrastructure.persistence.repository.MerchantPayoutRepository;
import com.aionn.payment.infrastructure.persistence.repository.PaymentRepository;
import java.time.Instant;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PaymentAnalyticsQueryAdapter implements PaymentAnalyticsQueryPort {
    private final PaymentRepository paymentRepository;
    private final MerchantPayoutRepository payoutRepository;

    @Override
    public AdminPaymentAnalyticsResult getAdminAnalytics(
            LocalDate from, LocalDate to, Instant fromInclusive, Instant toExclusive, String currency) {
        var paymentSummary = paymentRepository.summarize(fromInclusive, toExclusive, currency);
        var payoutSummary = payoutRepository.summarizeCompleted(fromInclusive, toExclusive, currency);
        long refundCount = valueOrZero(paymentSummary.getRefundCount());
        long paidCount = valueOrZero(paymentSummary.getPaidCount());
        return new AdminPaymentAnalyticsResult(
                from, to, currency, paymentSummary.getRefundAmount(), refundCount, paidCount,
                paidCount == 0 ? 0.0 : (double) refundCount / paidCount,
                payoutSummary.getAmount(), valueOrZero(payoutSummary.getCount()),
                payoutRepository.completedTrend(fromInclusive, toExclusive, currency).stream()
                        .map(row -> new DailyAmount(row.getDate(), row.getAmount(), valueOrZero(row.getCount())))
                        .toList(),
                paymentRepository.countByStatus(fromInclusive, toExclusive, currency).stream()
                        .map(row -> new StatusCount(row.getStatus(), valueOrZero(row.getCount())))
                        .toList(),
                payoutRepository.countByStatus(fromInclusive, toExclusive, currency).stream()
                        .map(row -> new StatusCount(row.getStatus(), valueOrZero(row.getCount())))
                        .toList());
    }

    private static long valueOrZero(Long value) {
        return value == null ? 0 : value;
    }
}
