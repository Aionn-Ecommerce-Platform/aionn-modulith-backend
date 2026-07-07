package com.aionn.identity.infrastructure.policy;

import com.aionn.identity.infrastructure.config.properties.AddressProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpringAddressPolicyTest {

    @Test
    void mapsMaxAddressNumbers() {
        SpringAddressPolicy policy = new SpringAddressPolicy(new AddressProperties(8));

        assertThat(policy.getMaxAddressNumbers()).isEqualTo(8L);
    }
}
