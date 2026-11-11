package com.aionn.ordering.infrastructure.policy;

import com.aionn.ordering.application.policy.OrderingAnalyticsPolicy;
import com.aionn.ordering.infrastructure.config.properties.OrderingAnalyticsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class SpringOrderingAnalyticsPolicy implements OrderingAnalyticsPolicy {

    private final OrderingAnalyticsProperties properties;

    @Override
    public ZoneId zone() {
        return ZoneId.of(properties.zone());
    }

    @Override
    public int merchantDefaultRangeDays() {
        return properties.merchantDefaultRangeDays();
    }

    @Override
    public int platformDefaultRangeDays() {
        return properties.platformDefaultRangeDays();
    }

    @Override
    public int topMerchantsLimit() {
        return properties.topMerchantsLimit();
    }

    @Override
    public int maxTopProductsLimit() {
        return properties.maxTopProductsLimit();
    }
}
