package com.aionn.config;

import com.aionn.identity.application.policy.IdentityValidationConstants;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("prod")
public class ProductionProviderConfigurationValidator {

    private final Environment environment;

    public ProductionProviderConfigurationValidator(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void validate() {
        List<String> errors = new ArrayList<>();

        requireMinimumLength(errors, "IDENTITY_JWT_SECRET", "identity.jwt.secret",
                IdentityValidationConstants.JWT_SECRET_MIN_LENGTH);
        requireRemoteProvider(errors, "identity.registration.captcha.provider", "google",
                "CAPTCHA_GOOGLE_SECRET_KEY", "identity.registration.captcha.google-secret-key");
        requireRemoteProvider(errors, "identity.auth.social.google.provider", "remote",
                "IDENTITY_AUTH_GOOGLE_CLIENT_ID", "identity.auth.social.google.client-id");

        if (is("identity.kyc.provider", "sumsub")) {
            require(errors, "IDENTITY_KYC_SUMSUB_APP_TOKEN", "identity.kyc.sumsub.app-token");
            require(errors, "IDENTITY_KYC_SUMSUB_SECRET_KEY", "identity.kyc.sumsub.secret-key");
            require(errors, "IDENTITY_KYC_SUMSUB_WEBHOOK_SECRET", "identity.kyc.sumsub.webhook-secret");
        }

        if (enabled("payment.provider.stripe.enabled", true)) {
            require(errors, "STRIPE_API_KEY", "payment.provider.stripe.api-key");
            require(errors, "STRIPE_WEBHOOK_SECRET", "payment.provider.stripe.webhook-secret");
        }
        if (enabled("payment.provider.vnpay.enabled", true)) {
            require(errors, "VNPAY_TMN_CODE", "payment.provider.vnpay.tmn-code");
            require(errors, "VNPAY_HASH_SECRET", "payment.provider.vnpay.hash-secret");
            require(errors, "VNPAY_RETURN_URL", "payment.provider.vnpay.return-url");
        }

        require(errors, "GHN_API_TOKEN", "shipping.carrier.ghn.token");
        require(errors, "GHN_SHOP_ID", "shipping.carrier.ghn.shop-id");
        require(errors, "GHN_WEBHOOK_SECRET", "shipping.carrier.ghn.webhook-secret");
        rejectLoggingProvider(errors, "notification.email.provider");
        rejectLoggingProvider(errors, "notification.sms.provider");
        rejectLoggingProvider(errors, "notification.push.provider");

        if (usesCloudinary()) {
            require(errors, "CLOUDINARY_CLOUD_NAME", "cloudinary.cloud-name");
            require(errors, "CLOUDINARY_API_KEY", "cloudinary.api-key");
            require(errors, "CLOUDINARY_API_SECRET", "cloudinary.api-secret");
        }

        rejectLocalhost(errors, "payment.invoice.base-url");
        rejectLocalhost(errors, "payment.provider.vnpay.return-url");
        rejectLocalhost(errors, "payment.provider.vnpay.frontend-return-url");
        rejectLocalhost(errors, "chat.websocket.allowed-origins");

        if (!errors.isEmpty()) {
            throw new IllegalStateException("Invalid production provider configuration: " + String.join("; ", errors));
        }
    }

    private void requireRemoteProvider(List<String> errors, String providerKey, String provider,
            String envName, String credentialKey) {
        if (is(providerKey, provider)) {
            require(errors, envName, credentialKey);
        }
    }

    private void require(List<String> errors, String envName, String key) {
        if (blank(environment.getProperty(key))) {
            errors.add(envName + " is required");
        }
    }

    private void requireMinimumLength(List<String> errors, String envName, String key, int minimumLength) {
        String value = environment.getProperty(key);
        if (blank(value)) {
            errors.add(envName + " is required");
        } else if (value.length() < minimumLength) {
            errors.add(envName + " must be at least " + minimumLength + " characters");
        }
    }

    private void rejectLoggingProvider(List<String> errors, String key) {
        String provider = environment.getProperty(key, "logging");
        if ("logging".equalsIgnoreCase(provider) || "noop".equalsIgnoreCase(provider)) {
            errors.add(key + " must use a real provider");
        }
    }

    private void rejectLocalhost(List<String> errors, String key) {
        String value = environment.getProperty(key);
        if (value != null && (value.contains("localhost") || value.contains("127.0.0.1"))) {
            errors.add(key + " must not target localhost");
        }
    }

    private boolean usesCloudinary() {
        return is("identity.media.provider", "cloudinary")
                || is("catalog.media.provider", "cloudinary")
                || is("promotion.media.provider", "cloudinary")
                || is("chat.media.provider", "cloudinary");
    }

    private boolean enabled(String key, boolean defaultValue) {
        return environment.getProperty(key, Boolean.class, defaultValue);
    }

    private boolean is(String key, String expected) {
        return expected.equalsIgnoreCase(environment.getProperty(key, ""));
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
