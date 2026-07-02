package com.aionn.identity.infrastructure.policy;

import com.aionn.identity.application.policy.KycPolicy;
import com.aionn.identity.infrastructure.config.properties.KycProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringKycPolicy implements KycPolicy {

    private final KycProperties properties;

    @Override
    public boolean isSumsubEnabled() {
        return properties.isSumsubEnabled();
    }

    @Override
    public boolean isLocalDevelopmentEnabled() {
        return properties.isLocalDevelopmentEnabled();
    }

    @Override
    public boolean usesManagedProvider() {
        return properties.usesManagedProvider();
    }
}
