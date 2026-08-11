package com.aionn.payment.adapter.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aionn.payment.adapter.rest.dto.analytics.response.AdminPaymentAnalyticsResponse;
import com.aionn.payment.adapter.rest.mapper.analytics.PaymentAnalyticsDtoMapper;
import com.aionn.payment.application.dto.analytics.query.GetAdminPaymentAnalyticsQuery;
import com.aionn.payment.application.dto.analytics.result.AdminPaymentAnalyticsResult;
import com.aionn.payment.application.port.in.analytics.GetAdminPaymentAnalyticsInputPort;
import com.aionn.sharedkernel.infrastructure.config.JacksonMapperFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminPaymentAnalyticsControllerWebTest {
    @Mock
    private GetAdminPaymentAnalyticsInputPort analyticsInputPort;
    @Mock
    private PaymentAnalyticsDtoMapper dtoMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new AdminPaymentAnalyticsController(analyticsInputPort, dtoMapper))
                .setMessageConverters(new JacksonJsonHttpMessageConverter(JacksonMapperFactory.create()))
                .build();
    }

    @Test
    void returnsAnalyticsForRequestedRangeAndCurrency() throws Exception {
        LocalDate from = LocalDate.parse("2026-08-01");
        LocalDate to = LocalDate.parse("2026-08-11");
        AdminPaymentAnalyticsResult result = new AdminPaymentAnalyticsResult(
                from, to, "USD", BigDecimal.TEN, 1, 4, 0.25,
                BigDecimal.valueOf(100), 2, List.of(), List.of(), List.of());
        AdminPaymentAnalyticsResponse response = new AdminPaymentAnalyticsResponse(
                from, to, "USD", BigDecimal.TEN, 1, 4, 0.25,
                BigDecimal.valueOf(100), 2, List.of(), List.of(), List.of());
        when(analyticsInputPort.execute(any(GetAdminPaymentAnalyticsQuery.class))).thenReturn(result);
        when(dtoMapper.toResponse(result)).thenReturn(response);

        mockMvc.perform(get("/api/v1/payments/admin/analytics")
                        .queryParam("from", from.toString())
                        .queryParam("to", to.toString())
                        .queryParam("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currency").value("USD"))
                .andExpect(jsonPath("$.data.totalPaidCount").value(4));

        verify(analyticsInputPort).execute(new GetAdminPaymentAnalyticsQuery(from, to, "USD"));
    }
}
