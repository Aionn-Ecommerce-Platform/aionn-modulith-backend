package com.aionn.payment.infrastructure.persistence.adapter.analytics;

import com.aionn.payment.application.dto.analytics.result.AdminPaymentAnalyticsResult;
import com.aionn.payment.application.dto.analytics.result.AdminPaymentAnalyticsResult.DailyAmount;
import com.aionn.payment.application.dto.analytics.result.AdminPaymentAnalyticsResult.StatusCount;
import com.aionn.payment.application.port.out.PaymentAnalyticsQueryPort;
import com.aionn.payment.infrastructure.persistence.entity.MerchantPayoutEntity;
import com.aionn.payment.infrastructure.persistence.entity.PaymentEntity;
import com.aionn.payment.infrastructure.persistence.repository.MerchantPayoutRepository;
import com.aionn.payment.infrastructure.persistence.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
        List<PaymentEntity> payments = paymentRepository
                .findByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndCurrency(
                        fromInclusive, toExclusive, currency);
        List<MerchantPayoutEntity> payouts = payoutRepository
                .findByRequestedAtGreaterThanEqualAndRequestedAtLessThanAndCurrency(
                        fromInclusive, toExclusive, currency);
        BigDecimal refundAmount = payments.stream().map(PaymentEntity::getRefundedAmount)
                .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        long refundCount = payments.stream().filter(payment -> payment.getRefundedAmount() != null
                && payment.getRefundedAmount().signum() > 0).count();
        long paidCount = payments.stream()
                .filter(payment -> "PAID".equals(payment.getStatus()) || "REFUNDED".equals(payment.getStatus()))
                .count();
        BigDecimal payoutAmount = payouts.stream().filter(payout -> "COMPLETED".equals(payout.getStatus()))
                .map(MerchantPayoutEntity::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        long payoutCount = payouts.stream().filter(payout -> "COMPLETED".equals(payout.getStatus())).count();
        return new AdminPaymentAnalyticsResult(from, to, currency, refundAmount, refundCount, paidCount,
                paidCount == 0 ? 0.0 : (double) refundCount / paidCount,
                payoutAmount, payoutCount, payoutTrend(payouts),
                statusCounts(payments.stream().map(PaymentEntity::getStatus).toList()),
                statusCounts(payouts.stream().map(MerchantPayoutEntity::getStatus).toList()));
    }

    private static List<DailyAmount> payoutTrend(List<MerchantPayoutEntity> payouts) {
        Map<LocalDate, List<MerchantPayoutEntity>> byDate = payouts.stream()
                .filter(payout -> "COMPLETED".equals(payout.getStatus()) && payout.getCompletedAt() != null)
                .collect(Collectors.groupingBy(
                        payout -> payout.getCompletedAt().atZone(ZoneOffset.UTC).toLocalDate()));
        return byDate.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(entry -> new DailyAmount(entry.getKey(), entry.getValue().stream()
                        .map(MerchantPayoutEntity::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
                        entry.getValue().size()))
                .toList();
    }

    private static List<StatusCount> statusCounts(List<String> statuses) {
        return statuses.stream().collect(Collectors.groupingBy(status -> status, Collectors.counting()))
                .entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(entry -> new StatusCount(entry.getKey(), entry.getValue())).toList();
    }
}
