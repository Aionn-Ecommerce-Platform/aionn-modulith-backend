package com.aionn.notification.infrastructure.config;

import com.aionn.notification.infrastructure.config.properties.NotificationEmailProperties;
import com.aionn.notification.infrastructure.config.properties.NotificationPushProperties;
import com.aionn.notification.infrastructure.config.properties.NotificationSmsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationProviderConfigurationValidator implements SmartInitializingSingleton {

    private static final Set<String> PRODUCTION_PROFILES = Set.of("prod", "production");

    private final NotificationEmailProperties emailProperties;
    private final NotificationSmsProperties smsProperties;
    private final NotificationPushProperties pushProperties;
    private final Environment environment;

    @Override
    public void afterSingletonsInstantiated() {
        validateEmail();
        validateSms();
        validatePush();
    }

    private void validateEmail() {
        String provider = emailProperties.provider();
        if ("logging".equalsIgnoreCase(provider)) {
            failOrWarn("notification.email.provider", provider,
                    "Configure smtp or remote provider before deploying.");
            return;
        }
        if ("smtp".equalsIgnoreCase(provider) && (emailProperties.from() == null || emailProperties.from().isBlank())) {
            String message = "notification.email.provider=smtp but 'from' address is not configured "
                    + "(NOTIFICATION_EMAIL_FROM).";
            if (isProductionProfile()) {
                throw new IllegalStateException(message);
            }
            log.warn(message);
        }
    }

    private void validateSms() {
        String provider = smsProperties.provider();
        if ("logging".equalsIgnoreCase(provider)) {
            failOrWarn("notification.sms.provider", provider,
                    "Configure twilio or remote provider before deploying.");
            return;
        }
        if ("twilio".equalsIgnoreCase(provider)) {
            NotificationSmsProperties.Twilio t = smsProperties.twilio();
            if (t == null || isBlank(t.accountSid()) || isBlank(t.authToken()) || isBlank(t.fromPhoneNumber())) {
                String message = "notification.sms.provider=twilio but credentials are missing "
                        + "(TWILIO_ACCOUNT_SID / TWILIO_AUTH_TOKEN / TWILIO_FROM_PHONE_NUMBER).";
                if (isProductionProfile()) {
                    throw new IllegalStateException(message);
                }
                log.warn(message);
            }
        }
    }

    private void validatePush() {
        if ("logging".equalsIgnoreCase(pushProperties.provider())) {
            failOrWarn("notification.push.provider", pushProperties.provider(),
                    "Configure a real push provider before deploying.");
        }
    }

    private void failOrWarn(String key, String provider, String advice) {
        if (isProductionProfile()) {
            throw new IllegalStateException(key + "=" + provider + " is not allowed in production. " + advice);
        }
        log.warn("{}={} - notifications will only be logged. {}", key, provider, advice);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isProductionProfile() {
        for (String profile : environment.getActiveProfiles()) {
            if (PRODUCTION_PROFILES.contains(profile.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
