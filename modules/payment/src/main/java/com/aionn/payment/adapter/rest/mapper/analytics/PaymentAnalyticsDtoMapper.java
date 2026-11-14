package com.aionn.payment.adapter.rest.mapper.analytics;

import com.aionn.payment.adapter.rest.dto.analytics.response.AdminPaymentAnalyticsResponse;
import com.aionn.payment.application.dto.analytics.query.GetAdminPaymentAnalyticsQuery;
import com.aionn.payment.application.dto.analytics.result.AdminPaymentAnalyticsResult;
import org.mapstruct.Mapper;

import java.time.LocalDate;

@Mapper(componentModel = "spring")
public interface PaymentAnalyticsDtoMapper {
    AdminPaymentAnalyticsResponse toResponse(AdminPaymentAnalyticsResult result);

    default GetAdminPaymentAnalyticsQuery toAnalyticsQuery(LocalDate from, LocalDate to, String currency) {
        return new GetAdminPaymentAnalyticsQuery(from, to, currency);
    }
}
