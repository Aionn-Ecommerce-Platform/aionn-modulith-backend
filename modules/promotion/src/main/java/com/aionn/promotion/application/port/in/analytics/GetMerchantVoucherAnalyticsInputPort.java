package com.aionn.promotion.application.port.in.analytics;

import com.aionn.promotion.application.dto.analytics.result.MerchantVoucherAnalyticsResult;

public interface GetMerchantVoucherAnalyticsInputPort {
    MerchantVoucherAnalyticsResult execute(String ownerId);
}
