package com.aionn.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionProviderConfigurationValidatorTest {

    @Test
    void rejectsDevelopmentFallbacksAndMissingCredentials() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("identity.registration.captcha.provider", "google")
                .withProperty("identity.auth.social.google.provider", "remote")
                .withProperty("identity.kyc.provider", "sumsub")
                .withProperty("identity.media.provider", "cloudinary")
                .withProperty("payment.provider.stripe.enabled", "true")
                .withProperty("payment.provider.vnpay.enabled", "true")
                .withProperty("notification.email.provider", "logging")
                .withProperty("payment.invoice.base-url", "http://localhost:8080/invoices");

        assertThatThrownBy(() -> new ProductionProviderConfigurationValidator(environment).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("IDENTITY_JWT_SECRET is required")
                .hasMessageContaining("CAPTCHA_GOOGLE_SECRET_KEY is required")
                .hasMessageContaining("IDENTITY_AUTH_GOOGLE_CLIENT_ID is required")
                .hasMessageContaining("IDENTITY_KYC_SUMSUB_APP_TOKEN is required")
                .hasMessageContaining("STRIPE_API_KEY is required")
                .hasMessageContaining("VNPAY_TMN_CODE is required")
                .hasMessageContaining("GHN_API_TOKEN is required")
                .hasMessageContaining("GHN_WEBHOOK_SECRET is required")
                .hasMessageContaining("notification.email.provider must use a real provider")
                .hasMessageContaining("notification.sms.provider must use a real provider")
                .hasMessageContaining("notification.push.provider must use a real provider")
                .hasMessageContaining("CLOUDINARY_CLOUD_NAME is required")
                .hasMessageContaining("SECURITY_CORS_ALLOWED_ORIGINS is required")
                .hasMessageContaining("CATALOG_SEARCH_OPENSEARCH_HOST is required")
                .hasMessageContaining("payment.invoice.base-url must not target localhost");
    }

    @Test
    void acceptsCompleteProductionProviderConfiguration() {
        MockEnvironment environment = completeEnvironment();

        assertDoesNotThrow(() -> new ProductionProviderConfigurationValidator(environment).validate());
    }

    @Test
    void rejectsWeakJwtSecretAndLocalVnpayReturnUrl() {
        MockEnvironment environment = completeEnvironment()
                .withProperty("identity.jwt.secret", "too-short")
                .withProperty("payment.provider.vnpay.return-url", "http://localhost:8080/payment/return");

        assertThatThrownBy(() -> new ProductionProviderConfigurationValidator(environment).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("IDENTITY_JWT_SECRET must be at least ")
                .hasMessageContaining("payment.provider.vnpay.return-url must not target localhost");
    }

    @Test
    void rejectsLocalBrowserAndSearchEndpoints() {
        MockEnvironment environment = completeEnvironment()
                .withProperty("security.cors.allowed-origins", "http://localhost:3000")
                .withProperty("catalog.search.opensearch.host", "127.0.0.1");

        assertThatThrownBy(() -> new ProductionProviderConfigurationValidator(environment).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("security.cors.allowed-origins must not target localhost")
                .hasMessageContaining("catalog.search.opensearch.host must not target localhost");
    }

    private static MockEnvironment completeEnvironment() {
        return new MockEnvironment()
                .withProperty("identity.jwt.secret", "long-production-secret-at-least-32-characters")
                .withProperty("identity.registration.captcha.provider", "google")
                .withProperty("identity.registration.captcha.google-secret-key", "captcha-secret")
                .withProperty("identity.auth.social.google.provider", "remote")
                .withProperty("identity.auth.social.google.client-id", "google-client")
                .withProperty("identity.kyc.provider", "sumsub")
                .withProperty("identity.kyc.sumsub.app-token", "sumsub-token")
                .withProperty("identity.kyc.sumsub.secret-key", "sumsub-secret")
                .withProperty("identity.kyc.sumsub.webhook-secret", "webhook-secret")
                .withProperty("payment.provider.stripe.enabled", "true")
                .withProperty("payment.provider.stripe.api-key", "stripe-key")
                .withProperty("payment.provider.stripe.webhook-secret", "stripe-webhook")
                .withProperty("payment.provider.vnpay.enabled", "true")
                .withProperty("payment.provider.vnpay.tmn-code", "tmn")
                .withProperty("payment.provider.vnpay.hash-secret", "hash")
                .withProperty("payment.provider.vnpay.return-url", "https://api.example.com/payment/return")
                .withProperty("shipping.carrier.ghn.token", "ghn-token")
                .withProperty("shipping.carrier.ghn.shop-id", "123")
                .withProperty("shipping.carrier.ghn.webhook-secret", "ghn-webhook-secret")
                .withProperty("notification.email.provider", "smtp")
                .withProperty("notification.sms.provider", "twilio")
                .withProperty("notification.push.provider", "firebase")
                .withProperty("identity.media.provider", "cloudinary")
                .withProperty("cloudinary.cloud-name", "cloud")
                .withProperty("cloudinary.api-key", "key")
                .withProperty("cloudinary.api-secret", "secret")
                .withProperty("payment.invoice.base-url", "https://api.example.com/invoices")
                .withProperty("payment.provider.vnpay.frontend-return-url", "https://shop.example.com/payment/return")
                .withProperty("chat.websocket.allowed-origins", "https://shop.example.com")
                .withProperty("security.cors.allowed-origins", "https://shop.example.com")
                .withProperty("catalog.search.provider", "opensearch")
                .withProperty("catalog.search.opensearch.host", "search.internal.example.com");
    }
}
