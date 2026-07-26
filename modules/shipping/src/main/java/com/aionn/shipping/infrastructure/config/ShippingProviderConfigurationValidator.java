package com.aionn.shipping.infrastructure.config;

import com.aionn.shipping.infrastructure.config.properties.ShippingCarrierProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShippingProviderConfigurationValidator implements SmartInitializingSingleton {

    private static final Set<String> PRODUCTION_PROFILES = Set.of("prod", "production");

    private final ShippingCarrierProperties carrierProperties;
    private final Environment environment;

    @Override
    public void afterSingletonsInstantiated() {
        validateCarrierProvider();
        validateGhnCredentials();
    }

    private void validateCarrierProvider() {
        String provider = carrierProperties.provider();
        if ("assume-success".equalsIgnoreCase(provider)) {
            if (isProductionProfile()) {
                throw new IllegalStateException(
                        "shipping.carrier.provider=assume-success is not allowed in production. "
                                + "Switch to ghn (or another real carrier) before deploying.");
            }
            log.warn("shipping.carrier.provider={} - shipments will use synthetic tracking codes. "
                    + "Switch to a real carrier before production.", provider);
        }
    }

    private void validateGhnCredentials() {
        if (!"ghn".equalsIgnoreCase(carrierProperties.provider())) {
            return;
        }
        ShippingCarrierProperties.Ghn ghn = carrierProperties.ghn();
        if (ghn == null || isBlank(ghn.apiToken()) || isBlank(ghn.shopId())) {
            String message = "GHN provider enabled but apiToken/shopId not configured "
                    + "(GHN_API_TOKEN / GHN_SHOP_ID).";
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
