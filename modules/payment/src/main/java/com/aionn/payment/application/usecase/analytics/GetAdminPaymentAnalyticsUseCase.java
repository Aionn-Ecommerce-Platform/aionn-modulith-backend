package com.aionn.payment.application.usecase.analytics;

import java.time.temporal.ChronoUnit;
import java.util.Locale;

import com.aionn.payment.application.dto.analytics.query.GetAdminPaymentAnalyticsQuery;
import com.aionn.payment.application.dto.analytics.result.AdminPaymentAnalyticsResult;
import com.aionn.payment.application.port.in.analytics.GetAdminPaymentAnalyticsInputPort;
import com.aionn.payment.application.port.out.PaymentAnalyticsQueryPort;
import com.aionn.payment.application.policy.PaymentAnalyticsPolicy;
import com.aionn.payment.application.policy.PaymentDefaultsPolicy;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetAdminPaymentAnalyticsUseCase implements GetAdminPaymentAnalyticsInputPort {

    private final PaymentAnalyticsQueryPort analyticsQueryPort;
    private final PaymentAnalyticsPolicy analyticsPolicy;
    private final PaymentDefaultsPolicy defaultsPolicy;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public AdminPaymentAnalyticsResult execute(GetAdminPaymentAnalyticsQuery query) {
        ZoneId zone = analyticsPolicy.zone();
        LocalDate to = query.to() == null ? clock.instant().atZone(zone).toLocalDate() : query.to();
        LocalDate from = query.from() == null
                ? to.minusDays(analyticsPolicy.defaultRangeDays() - 1L)
                : query.from();
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
        long maxRangeDays = analyticsPolicy.maxRangeDays();
        if (ChronoUnit.DAYS.between(from, to) + 1 > maxRangeDays) {
            throw new IllegalArgumentException("analytics range must not exceed " + maxRangeDays + " days");
        }
        String currency = query.currency() == null || query.currency().isBlank()
                ? defaultsPolicy.defaultCurrency()
                : query.currency().toUpperCase(Locale.ROOT);
        return analyticsQueryPort.getAdminAnalytics(from, to,
                from.atStartOfDay(zone).toInstant(),
                to.plusDays(1).atStartOfDay(zone).toInstant(), currency);
    }
}
