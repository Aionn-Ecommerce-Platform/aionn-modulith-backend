package com.aionn.promotion.application.usecase.analytics;

import com.aionn.promotion.application.dto.analytics.result.MerchantVoucherAnalyticsResult;
import com.aionn.promotion.application.port.in.analytics.GetMerchantVoucherAnalyticsInputPort;
import com.aionn.promotion.application.service.VoucherAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetMerchantVoucherAnalyticsUseCase implements GetMerchantVoucherAnalyticsInputPort {

    private final VoucherAnalyticsService voucherAnalyticsService;

    @Override
    @Transactional(readOnly = true)
    public MerchantVoucherAnalyticsResult execute(String ownerId) {
        return voucherAnalyticsService.getMerchantAnalytics(ownerId);
    }
}
