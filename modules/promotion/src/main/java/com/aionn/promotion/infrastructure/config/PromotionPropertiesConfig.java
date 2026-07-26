package com.aionn.promotion.infrastructure.config;

import com.aionn.promotion.infrastructure.config.properties.PromotionDefaultsProperties;
import com.aionn.promotion.infrastructure.config.properties.PromotionSchedulerProperties;
import com.aionn.promotion.infrastructure.config.properties.PromotionVoucherProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        PromotionDefaultsProperties.class,
        PromotionVoucherProperties.class,
        PromotionSchedulerProperties.class
})
public class PromotionPropertiesConfig {
}
