package com.aionn.promotion.infrastructure.config;

import com.aionn.promotion.infrastructure.config.properties.PromotionCloudinaryProperties;
import com.aionn.promotion.infrastructure.config.properties.PromotionMediaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({PromotionMediaProperties.class, PromotionCloudinaryProperties.class})
public class PromotionMediaConfig {
}
