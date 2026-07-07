package com.aionn.catalog.application.port.in.product;

import com.aionn.catalog.application.dto.analytics.result.ProductAnalyticsResult;

public interface GetProductAnalyticsInputPort {

    ProductAnalyticsResult execute();
}
