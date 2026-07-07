package com.aionn.identity.infrastructure.policy;

import com.aionn.identity.domain.valueobject.KycProvider;
import com.aionn.identity.infrastructure.config.properties.KycProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpringKycPolicyTest {

    @Test
    void reflectsLocalProvider() {
        SpringKycPolicy policy = new SpringKycPolicy(new KycProperties(KycProvider.LOCAL, null, null));

        assertThat(policy.isLocalDevelopmentEnabled()).isTrue();
        assertThat(policy.isSumsubEnabled()).isFalse();
        assertThat(policy.usesManagedProvider()).isFalse();
    }

    @Test
    void reflectsSumsubProvider() {
        SpringKycPolicy policy = new SpringKycPolicy(new KycProperties(KycProvider.SUMSUB, null, null));

        assertThat(policy.isSumsubEnabled()).isTrue();
        assertThat(policy.isLocalDevelopmentEnabled()).isFalse();
        assertThat(policy.usesManagedProvider()).isTrue();
    }
}
