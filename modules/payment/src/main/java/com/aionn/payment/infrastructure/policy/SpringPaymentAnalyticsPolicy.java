package com.aionn.payment.infrastructure.policy;

import com.aionn.payment.application.policy.PaymentAnalyticsPolicy;
import com.aionn.payment.infrastructure.config.properties.PaymentAnalyticsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class SpringPaymentAnalyticsPolicy implements PaymentAnalyticsPolicy {

    private final PaymentAnalyticsProperties properties;

    @Override
    public ZoneId zone() {
        return ZoneId.of(properties.zone());
    }

    @Override
    public int defaultRangeDays() {
        return properties.defaultRangeDays();
    }

    @Override
    public long maxRangeDays() {
        return properties.maxRangeDays();
    }
}
