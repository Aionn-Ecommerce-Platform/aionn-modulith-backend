package com.aionn.payment.application.usecase.analytics;

import com.aionn.payment.application.dto.analytics.query.GetAdminPaymentAnalyticsQuery;
import com.aionn.payment.application.dto.analytics.result.AdminPaymentAnalyticsResult;
import com.aionn.payment.application.port.in.analytics.GetAdminPaymentAnalyticsInputPort;
import com.aionn.payment.application.port.out.PaymentAnalyticsQueryPort;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetAdminPaymentAnalyticsUseCase implements GetAdminPaymentAnalyticsInputPort {
    private final PaymentAnalyticsQueryPort analyticsQueryPort;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public AdminPaymentAnalyticsResult execute(GetAdminPaymentAnalyticsQuery query) {
        LocalDate to = query.to() == null ? clock.instant().atZone(ZoneOffset.UTC).toLocalDate() : query.to();
        LocalDate from = query.from() == null ? to.minusDays(29) : query.from();
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
        String currency = query.currency() == null || query.currency().isBlank()
                ? "VND"
                : query.currency().toUpperCase(java.util.Locale.ROOT);
        return analyticsQueryPort.getAdminAnalytics(from, to,
                from.atStartOfDay(ZoneOffset.UTC).toInstant(),
                to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant(), currency);
    }
}
