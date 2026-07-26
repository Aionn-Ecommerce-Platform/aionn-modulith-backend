package com.aionn.promotion.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "promotion.media.cloudinary")
public record PromotionCloudinaryProperties(
        @DefaultValue("aionn/promotion/banners") String bannerFolder) {
}
