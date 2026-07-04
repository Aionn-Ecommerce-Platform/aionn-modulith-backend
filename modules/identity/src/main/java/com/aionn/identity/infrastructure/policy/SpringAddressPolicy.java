package com.aionn.identity.infrastructure.policy;

import com.aionn.identity.application.policy.AddressPolicy;
import com.aionn.identity.infrastructure.config.properties.AddressProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringAddressPolicy implements AddressPolicy {

    private final AddressProperties addressProperties;

    @Override
    public long getMaxAddressNumbers() {
        return addressProperties.maxAddressNumbers();
    }
}
