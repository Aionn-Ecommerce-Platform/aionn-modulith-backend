package com.aionn.identity.infrastructure.policy;

import com.aionn.identity.infrastructure.config.properties.MfaProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpringMfaPolicyTest {

    @Test
    void mapsIssuerAndBackupCodeCount() {
        SpringMfaPolicy policy = new SpringMfaPolicy(new MfaProperties("Aionn", "key", 10));

        assertThat(policy.getMfaIssuer()).isEqualTo("Aionn");
        assertThat(policy.getBackupCodeCount()).isEqualTo(10);
    }
}
