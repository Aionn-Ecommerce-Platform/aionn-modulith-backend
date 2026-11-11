package com.aionn.ordering.infrastructure.policy;

import com.aionn.ordering.infrastructure.config.properties.OrderingAnalyticsProperties;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class SpringOrderingAnalyticsPolicyTest {

    @Test
    void exposesConfiguredAnalyticsSettings() {
        var policy = new SpringOrderingAnalyticsPolicy(new OrderingAnalyticsProperties(
                "Asia/Bangkok", 7, 30, 10, 20));

        assertThat(policy.zone()).isEqualTo(ZoneId.of("Asia/Bangkok"));
        assertThat(policy.merchantDefaultRangeDays()).isEqualTo(7);
        assertThat(policy.platformDefaultRangeDays()).isEqualTo(30);
        assertThat(policy.topMerchantsLimit()).isEqualTo(10);
        assertThat(policy.maxTopProductsLimit()).isEqualTo(20);
    }
}
