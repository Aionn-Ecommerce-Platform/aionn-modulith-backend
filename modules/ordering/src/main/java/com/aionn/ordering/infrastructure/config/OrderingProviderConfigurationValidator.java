package com.aionn.ordering.infrastructure.config;

import com.aionn.ordering.infrastructure.config.properties.OrderingCatalogPricingProperties;
import com.aionn.ordering.infrastructure.config.properties.OrderingPaymentProperties;
import com.aionn.ordering.infrastructure.config.properties.OrderingShippingProperties;
import com.aionn.ordering.infrastructure.config.properties.OrderingVoucherProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderingProviderConfigurationValidator implements SmartInitializingSingleton {

    private static final Set<String> PRODUCTION_PROFILES = Set.of("prod", "production");

    private final OrderingPaymentProperties paymentProperties;
    private final OrderingShippingProperties shippingProperties;
    private final OrderingCatalogPricingProperties catalogPricingProperties;
    private final OrderingVoucherProperties voucherProperties;
    private final Environment environment;

    @Override
    public void afterSingletonsInstantiated() {
        validatePayment();
        validateShipping();
        validateCatalogPricing();
        validateVoucher();
    }

    private void validatePayment() {
        String provider = paymentProperties.provider();
        if ("assume-success".equalsIgnoreCase(provider)) {
            failOrWarn("ordering.payment.provider", provider,
                    "Configure a real payment provider (remote) before deploying.");
        }
    }

    private void validateShipping() {
        String provider = shippingProperties.provider();
        if ("assume-success".equalsIgnoreCase(provider)) {
            failOrWarn("ordering.shipping.provider", provider,
                    "Configure a real shipping provider (remote) before deploying.");
        }
    }

    private void validateCatalogPricing() {
        String provider = catalogPricingProperties.provider();
        if ("assume-available".equalsIgnoreCase(provider)) {
            failOrWarn("ordering.catalog-pricing.provider", provider,
                    "Configure a real catalog-pricing provider (remote) before deploying.");
        }
    }

    private void validateVoucher() {
        String provider = voucherProperties.provider();
        if ("no-discount".equalsIgnoreCase(provider)) {
            failOrWarn("ordering.voucher.provider", provider,
                    "Configure a real voucher provider (remote) before deploying.");
        }
    }

    private void failOrWarn(String key, String provider, String advice) {
        if (isProductionProfile()) {
            throw new IllegalStateException(
                    key + "=" + provider + " is not allowed in production. " + advice);
        }
        log.warn("{}={} - placeholder provider in use. {}", key, provider, advice);
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
