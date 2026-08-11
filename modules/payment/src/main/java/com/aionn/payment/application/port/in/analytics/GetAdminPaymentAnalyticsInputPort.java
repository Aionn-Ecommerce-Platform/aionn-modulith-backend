package com.aionn.payment.application.port.in.analytics;

import com.aionn.payment.application.dto.analytics.query.GetAdminPaymentAnalyticsQuery;
import com.aionn.payment.application.dto.analytics.result.AdminPaymentAnalyticsResult;

public interface GetAdminPaymentAnalyticsInputPort {
    AdminPaymentAnalyticsResult execute(GetAdminPaymentAnalyticsQuery query);
}
