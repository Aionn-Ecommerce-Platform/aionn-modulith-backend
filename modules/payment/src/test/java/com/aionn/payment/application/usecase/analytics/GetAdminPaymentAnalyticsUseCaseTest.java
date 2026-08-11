package com.aionn.payment.application.usecase.analytics;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aionn.payment.application.dto.analytics.query.GetAdminPaymentAnalyticsQuery;
import com.aionn.payment.application.dto.analytics.result.AdminPaymentAnalyticsResult;
import com.aionn.payment.application.port.out.PaymentAnalyticsQueryPort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class GetAdminPaymentAnalyticsUseCaseTest {
    private final PaymentAnalyticsQueryPort queryPort = mock(PaymentAnalyticsQueryPort.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-11T10:00:00Z"), ZoneOffset.UTC);
    private final GetAdminPaymentAnalyticsUseCase useCase =
            new GetAdminPaymentAnalyticsUseCase(queryPort, clock);

    @Test
    void defaultsToLastThirtyDaysAndVnd() {
        when(queryPort.getAdminAnalytics(any(), any(), any(), any(), any()))
                .thenReturn(mock(AdminPaymentAnalyticsResult.class));

        useCase.execute(new GetAdminPaymentAnalyticsQuery(null, null, null));

        verify(queryPort).getAdminAnalytics(
                eq(LocalDate.parse("2026-07-13")), eq(LocalDate.parse("2026-08-11")),
                eq(Instant.parse("2026-07-13T00:00:00Z")), eq(Instant.parse("2026-08-12T00:00:00Z")),
                eq("VND"));
    }

    @Test
    void rejectsInvertedRange() {
        assertThatThrownBy(() -> useCase.execute(new GetAdminPaymentAnalyticsQuery(
                LocalDate.parse("2026-08-12"), LocalDate.parse("2026-08-11"), "VND")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("from must not be after to");
    }
}
