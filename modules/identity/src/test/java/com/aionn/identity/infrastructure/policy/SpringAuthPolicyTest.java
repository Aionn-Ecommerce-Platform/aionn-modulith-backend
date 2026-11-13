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

        assertThat(policy.sessionExpiresDays()).isEqualTo(45L);
        assertThat(policy.accessTokenExpiryMinutes()).isEqualTo(20);
        assertThat(policy.maxFailedLoginAttempts()).isEqualTo(7);
        assertThat(policy.lockoutMinutes()).isEqualTo(12);
        assertThat(policy.passwordResetTokenTtlMinutes()).isEqualTo(25);
        assertThat(policy.loginIpMaxAttempts()).isEqualTo(30);
        assertThat(policy.loginIdentityMaxAttempts()).isEqualTo(10);
        assertThat(policy.passwordResetIpMaxAttempts()).isEqualTo(10);
        assertThat(policy.passwordResetIdentityMaxAttempts()).isEqualTo(3);
    }
}
