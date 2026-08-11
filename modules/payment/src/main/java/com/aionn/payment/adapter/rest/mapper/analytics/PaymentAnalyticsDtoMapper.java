package com.aionn.payment.adapter.rest.mapper.analytics;

import com.aionn.payment.adapter.rest.dto.analytics.response.AdminPaymentAnalyticsResponse;
import com.aionn.payment.application.dto.analytics.result.AdminPaymentAnalyticsResult;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentAnalyticsDtoMapper {
    AdminPaymentAnalyticsResponse toResponse(AdminPaymentAnalyticsResult result);
}
