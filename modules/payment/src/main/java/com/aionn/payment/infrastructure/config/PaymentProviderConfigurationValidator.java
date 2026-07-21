package com.aionn.payment.infrastructure.config;

import com.aionn.payment.infrastructure.config.properties.PaymentInvoiceProperties;
import com.aionn.payment.infrastructure.config.properties.PaymentStripeProperties;
import com.aionn.payment.infrastructure.config.properties.PaymentVnpayProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProviderConfigurationValidator implements SmartInitializingSingleton {

    private static final Set<String> PRODUCTION_PROFILES = Set.of("prod", "production");

    private final PaymentStripeProperties stripeProperties;
    private final PaymentVnpayProperties vnpayProperties;
    private final PaymentInvoiceProperties invoiceProperties;
    private final Environment environment;

    @Override
    public void afterSingletonsInstantiated() {
        validateProviders();
        validateInvoice();
        validateStripeCredentials();
        validateVnpayCredentials();
    }

    private void validateProviders() {
        if (!stripeProperties.enabled() && !vnpayProperties.enabled()) {
            throw new IllegalStateException(
                    "No payment provider is enabled. Enable at least one of "
                            + "payment.provider.{stripe,vnpay}.enabled.");
        }
    }

    private void validateInvoice() {
        if ("local".equalsIgnoreCase(invoiceProperties.provider()) && isProductionProfile()) {
            throw new IllegalStateException(
                    "payment.invoice.provider=local is not allowed in production. "
                            + "Switch to a remote invoice provider before deploying.");
        }
    }

    private void validateStripeCredentials() {
        if (!stripeProperties.enabled()) {
            return;
        }
        if (isBlank(stripeProperties.apiKey()) || isBlank(stripeProperties.webhookSecret())) {
            String message = "Stripe enabled but apiKey/webhookSecret not configured "
                    + "(STRIPE_API_KEY / STRIPE_WEBHOOK_SECRET).";
            if (isProductionProfile()) {
                throw new IllegalStateException(message);
            }
            log.warn(message);
        }
    }

    private void validateVnpayCredentials() {
        if (!vnpayProperties.enabled()) {
            return;
        }
        if (isBlank(vnpayProperties.tmnCode()) || isBlank(vnpayProperties.hashSecret())) {
            String message = "VNPay enabled but tmnCode/hashSecret not configured "
                    + "(VNPAY_TMN_CODE / VNPAY_HASH_SECRET).";
            if (isProductionProfile()) {
                throw new IllegalStateException(message);
            }
            log.warn(message);
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
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
