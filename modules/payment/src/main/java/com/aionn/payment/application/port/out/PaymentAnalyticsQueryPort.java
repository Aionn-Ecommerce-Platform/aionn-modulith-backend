package com.aionn.payment.application.port.out;

import com.aionn.payment.application.dto.analytics.result.AdminPaymentAnalyticsResult;
import java.time.Instant;
import java.time.LocalDate;

public interface PaymentAnalyticsQueryPort {
    AdminPaymentAnalyticsResult getAdminAnalytics(
            LocalDate from, LocalDate to, Instant fromInclusive, Instant toExclusive, String currency);
}
