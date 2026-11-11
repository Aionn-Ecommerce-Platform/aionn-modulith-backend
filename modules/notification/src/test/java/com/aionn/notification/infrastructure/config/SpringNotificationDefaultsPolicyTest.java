package com.aionn.notification.infrastructure.config;

import com.aionn.notification.infrastructure.config.properties.NotificationDefaultsProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpringNotificationDefaultsPolicyTest {

    @Test
    void exposesConfiguredLocales() {
        var policy = new SpringNotificationDefaultsPolicy(
                new NotificationDefaultsProperties(" en-GB ", " en-AU "));

        assertThat(policy.defaultLocale()).isEqualTo("en-GB");
        assertThat(policy.englishLocale()).isEqualTo("en-AU");
    }

    @Test
    void fallsBackWhenLocalesAreBlank() {
        var policy = new SpringNotificationDefaultsPolicy(
                new NotificationDefaultsProperties(" ", null));

        assertThat(policy.defaultLocale()).isEqualTo("vi-VN");
        assertThat(policy.englishLocale()).isEqualTo("en-US");
    }
}
