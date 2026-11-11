package com.aionn.shipping.infrastructure.config;

import com.aionn.shipping.infrastructure.carrier.config.GhnProperties;
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

    private static final Set<String> PRODUCTION_PROFILES = Set.of("prod");

    private final GhnProperties ghn;
    private final Environment environment;

    @Override
    public void afterSingletonsInstantiated() {
        validateGhnCredentials();
    }

    private void validateGhnCredentials() {
        if (!isBlank(ghn.baseUrl()) && !isBlank(ghn.token()) && !isBlank(ghn.shopId())
                && ghn.fromDistrictId() != null && !isBlank(ghn.fromWardCode())) {
            return;
        }
        String message = "GHN is the only shipping carrier but its configuration is incomplete "
                + "(GHN_API_TOKEN / GHN_SHOP_ID / GHN_FROM_DISTRICT_ID / GHN_FROM_WARD_CODE).";
        if (isProductionProfile()) {
            throw new IllegalStateException(message);
        }
        log.warn(message);
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
