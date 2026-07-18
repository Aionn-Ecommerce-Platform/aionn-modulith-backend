package com.aionn.ordering.application.port.in.order;

import com.aionn.ordering.application.dto.order.result.MerchantOrderAnalyticsResult;
import java.time.LocalDate;

public interface GetMerchantOrderAnalyticsInputPort {
    MerchantOrderAnalyticsResult execute(String ownerId, LocalDate from, LocalDate to);
}