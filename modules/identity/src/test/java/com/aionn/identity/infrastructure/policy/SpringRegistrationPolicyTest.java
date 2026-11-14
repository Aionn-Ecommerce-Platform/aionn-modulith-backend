package com.aionn.identity.infrastructure.policy;

import com.aionn.identity.infrastructure.config.properties.RegistrationProperties;
import com.aionn.identity.infrastructure.config.properties.RegistrationProperties.Captcha;
import com.aionn.identity.infrastructure.config.properties.RegistrationProperties.RateLimit;
import com.aionn.identity.infrastructure.config.properties.RegistrationProperties.Twilio;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpringRegistrationPolicyTest {

    private RegistrationProperties properties(boolean exposeOtp) {
        return new RegistrationProperties(
                6,
                90,
                240,
                45,
                60L,
                "+1",
                new RateLimit(4, 400, 2, 120, 10, 300, 5, 300),
                new Captcha("google", "", "", ""),
                new Twilio(false, "", "", ""),
                exposeOtp);
    }

    @Test
    void mapsScalarAndNestedValues() {
        SpringRegistrationPolicy policy = new SpringRegistrationPolicy(properties(false));

        assertThat(policy.maxVerifyAttempts()).isEqualTo(6);
        assertThat(policy.resendCooldownSeconds()).isEqualTo(90);
        assertThat(policy.otpExpirySeconds()).isEqualTo(240);
        assertThat(policy.lockTimeoutSeconds()).isEqualTo(45);
        assertThat(policy.sessionExpiresDays()).isEqualTo(60L);
        assertThat(policy.defaultCountryCallingCode()).isEqualTo("+1");
        assertThat(policy.ipRateLimitMaxAttempts()).isEqualTo(4);
        assertThat(policy.ipRateLimitWindowSeconds()).isEqualTo(400);
        assertThat(policy.phoneRateLimitMaxAttempts()).isEqualTo(2);
        assertThat(policy.phoneRateLimitWindowSeconds()).isEqualTo(120);
    }

    @Test
    void exposesOtpOnlyWhenExplicitlyEnabled() {
        SpringRegistrationPolicy policy = new SpringRegistrationPolicy(properties(true));

        assertThat(policy.isExposeOtpInResponse()).isTrue();
    }

    @Test
    void hidesOtpByDefault() {
        SpringRegistrationPolicy policy = new SpringRegistrationPolicy(properties(false));

        assertThat(policy.isExposeOtpInResponse()).isFalse();
    }
}
