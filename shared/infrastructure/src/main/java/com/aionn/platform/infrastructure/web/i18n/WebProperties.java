package com.aionn.platform.infrastructure.web.i18n;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "aionn.web")
public record WebProperties(
        @DefaultValue("vi") String defaultLocale) {
}
