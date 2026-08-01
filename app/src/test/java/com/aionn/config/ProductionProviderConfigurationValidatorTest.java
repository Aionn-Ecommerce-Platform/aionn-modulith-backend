package com.aionn.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductionProviderConfigurationValidatorTest {

    @Test
    void rejectsDevelopmentFallbacksAndMissingCredentials() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("identity.registration.captcha.provider", "google")
                .withProperty("payment.provider.stripe.enabled", "true")
                .withProperty("payment.provider.vnpay.enabled", "true")
                .withProperty("notification.email.provider", "logging")
                .withProperty("payment.invoice.base-url", "http://localhost:8080/invoices");

        assertThrows(IllegalStateException.class,
                () -> new ProductionProviderConfigurationValidator(environment).validate());
    }

    @Test
    void acceptsCompleteProductionProviderConfiguration() {
        MockEnvironment environment = completeEnvironment();

        assertDoesNotThrow(() -> new ProductionProviderConfigurationValidator(environment).validate());
    }

    private static MockEnvironment completeEnvironment() {
        return new MockEnvironment()
                .withProperty("identity.jwt.secret", "long-production-secret")
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
                .withProperty("notification.email.provider", "smtp")
                .withProperty("notification.sms.provider", "twilio")
                .withProperty("notification.push.provider", "firebase")
                .withProperty("identity.media.provider", "cloudinary")
                .withProperty("cloudinary.cloud-name", "cloud")
                .withProperty("cloudinary.api-key", "key")
                .withProperty("cloudinary.api-secret", "secret")
                .withProperty("payment.invoice.base-url", "https://api.example.com/invoices")
                .withProperty("payment.provider.vnpay.frontend-return-url", "https://shop.example.com/payment/return")
                .withProperty("chat.websocket.allowed-origins", "https://shop.example.com");
    }
}
