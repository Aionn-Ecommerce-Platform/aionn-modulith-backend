package com.aionn.catalog.application.port.out.analytics;

import com.aionn.catalog.application.dto.analytics.result.ProductAnalyticsResult;

public interface ProductAnalyticsQueryPort {

    ProductAnalyticsResult getProductAnalytics();
}
