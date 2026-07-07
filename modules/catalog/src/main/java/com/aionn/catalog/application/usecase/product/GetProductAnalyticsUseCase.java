package com.aionn.catalog.application.usecase.product;

import com.aionn.catalog.application.dto.analytics.result.ProductAnalyticsResult;
import com.aionn.catalog.application.port.in.product.GetProductAnalyticsInputPort;
import com.aionn.catalog.application.port.out.analytics.ProductAnalyticsQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetProductAnalyticsUseCase implements GetProductAnalyticsInputPort {

    private final ProductAnalyticsQueryPort productAnalyticsQueryPort;

    @Override
    @Transactional(readOnly = true)
    public ProductAnalyticsResult execute() {
        return productAnalyticsQueryPort.getProductAnalytics();
    }
}
