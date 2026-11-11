package com.aionn.notification.infrastructure.config;

import com.aionn.notification.application.policy.NotificationDefaultsPolicy;
import com.aionn.notification.infrastructure.config.properties.NotificationDefaultsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringNotificationDefaultsPolicy implements NotificationDefaultsPolicy {

    private static final String FALLBACK_LOCALE = "vi-VN";
    private static final String FALLBACK_ENGLISH_LOCALE = "en-US";

    private final NotificationDefaultsProperties properties;

    @Override
    public String defaultLocale() {
        return normalize(properties.locale(), FALLBACK_LOCALE);
    }

    @Override
    public String englishLocale() {
        return normalize(properties.englishLocale(), FALLBACK_ENGLISH_LOCALE);
    }

    private static String normalize(String locale, String fallback) {
        return (locale == null || locale.isBlank()) ? fallback : locale.trim();
    }
}
