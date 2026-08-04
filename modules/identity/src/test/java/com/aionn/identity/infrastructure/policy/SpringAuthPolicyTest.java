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
        AuthProperties auth = new AuthProperties(
                "X-Client-Type", "mobile", 7, 12, 25,
                30, 10, 300, 10, 3, 900);

        SpringAuthPolicy policy = new SpringAuthPolicy(session, jwt, auth);

        assertThat(policy.getSessionExpiresDays()).isEqualTo(45L);
        assertThat(policy.getAccessTokenExpiryMinutes()).isEqualTo(20);
        assertThat(policy.getMaxFailedLoginAttempts()).isEqualTo(7);
        assertThat(policy.getLockoutMinutes()).isEqualTo(12);
        assertThat(policy.getPasswordResetTokenTtlMinutes()).isEqualTo(25);
        assertThat(policy.getLoginIpMaxAttempts()).isEqualTo(30);
        assertThat(policy.getLoginIdentityMaxAttempts()).isEqualTo(10);
        assertThat(policy.getPasswordResetIpMaxAttempts()).isEqualTo(10);
        assertThat(policy.getPasswordResetIdentityMaxAttempts()).isEqualTo(3);
    }
}
