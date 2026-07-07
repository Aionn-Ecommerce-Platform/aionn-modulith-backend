package com.aionn.identity.infrastructure.policy;

import com.aionn.identity.infrastructure.config.properties.AuthProperties;
import com.aionn.identity.infrastructure.config.properties.AuthSessionProperties;
import com.aionn.identity.infrastructure.config.properties.JwtProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpringAuthPolicyTest {

    @Test
    void mapsValuesFromUnderlyingProperties() {
        AuthSessionProperties session = new AuthSessionProperties(45L);
        JwtProperties jwt = new JwtProperties("aionn-identity", "secret", 20);
        AuthProperties auth = new AuthProperties("X-Client-Type", "mobile", 7, 12, 25);

        SpringAuthPolicy policy = new SpringAuthPolicy(session, jwt, auth);

        assertThat(policy.getSessionExpiresDays()).isEqualTo(45L);
        assertThat(policy.getAccessTokenExpiryMinutes()).isEqualTo(20);
        assertThat(policy.getMaxFailedLoginAttempts()).isEqualTo(7);
        assertThat(policy.getLockoutMinutes()).isEqualTo(12);
        assertThat(policy.getPasswordResetTokenTtlMinutes()).isEqualTo(25);
    }
}
