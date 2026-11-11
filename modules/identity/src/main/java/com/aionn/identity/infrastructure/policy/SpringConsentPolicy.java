package com.aionn.identity.infrastructure.policy;

import com.aionn.identity.application.policy.ConsentPolicy;
import com.aionn.identity.infrastructure.config.properties.ConsentProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringConsentPolicy implements ConsentPolicy {

    private final ConsentProperties properties;

    @Override
    public String termsVersion() {
        return properties.termsVersion();
    }

    @Override
    public String privacyVersion() {
        return properties.privacyVersion();
    }

    @Override
    public String marketingVersion() {
        return properties.marketingVersion();
    }
}
