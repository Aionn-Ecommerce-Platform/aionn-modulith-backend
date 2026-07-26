package com.aionn.promotion.infrastructure.config;

import com.aionn.promotion.infrastructure.config.properties.PromotionCloudinaryProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PromotionCloudinaryProperties.class)
public class PromotionMediaConfig {
}
