package com.aionn.payment.application.dto.analytics.query;

import com.aionn.sharedkernel.application.query.Query;
import java.time.LocalDate;

public record GetAdminPaymentAnalyticsQuery(LocalDate from, LocalDate to, String currency) implements Query {
}
