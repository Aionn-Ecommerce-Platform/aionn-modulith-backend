package com.aionn.promotion.infrastructure.config;

import com.aionn.promotion.infrastructure.config.properties.PromotionVoucherProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromotionConfigurationValidator implements SmartInitializingSingleton {

    private final PromotionVoucherProperties voucherProperties;

    @Override
    public void afterSingletonsInstantiated() {
        if (voucherProperties.reservationTtlSeconds() < 60) {
            throw new IllegalStateException(
                    "promotion.voucher.reservation-ttl-seconds must be >= 60. Current value: "
                            + voucherProperties.reservationTtlSeconds());
        }
        if (voucherProperties.autoRelease().batchSize() <= 0) {
            log.warn("promotion.voucher.auto-release.batch-size <= 0; falling back to 100");
        }
    }
}
