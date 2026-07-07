package com.aionn.identity.infrastructure.policy;

import com.aionn.identity.infrastructure.config.properties.RegistrationProperties;
import com.aionn.identity.infrastructure.config.properties.RegistrationProperties.Captcha;
import com.aionn.identity.infrastructure.config.properties.RegistrationProperties.RateLimit;
import com.aionn.identity.infrastructure.config.properties.RegistrationProperties.Twilio;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpringRegistrationPolicyTest {

    private RegistrationProperties properties(boolean twilioEnabled) {
        return new RegistrationProperties(
                6,
                90,
                240,
                45,
                60L,
                "+1",
                new RateLimit(4, 400, 2, 120),
                new Captcha("google", "", "", ""),
                new Twilio(twilioEnabled, "", "", ""));
    }

    @Test
    void mapsScalarAndNestedValues() {
        SpringRegistrationPolicy policy = new SpringRegistrationPolicy(properties(false));

        assertThat(policy.getMaxVerifyAttempts()).isEqualTo(6);
        assertThat(policy.getResendCooldownSeconds()).isEqualTo(90);
        assertThat(policy.getOtpExpirySeconds()).isEqualTo(240);
        assertThat(policy.getLockTimeoutSeconds()).isEqualTo(45);
        assertThat(policy.getSessionExpiresDays()).isEqualTo(60L);
        assertThat(policy.getDefaultCountryCallingCode()).isEqualTo("+1");
        assertThat(policy.getIpRateLimitMaxAttempts()).isEqualTo(4);
        assertThat(policy.getIpRateLimitWindowSeconds()).isEqualTo(400);
        assertThat(policy.getPhoneRateLimitMaxAttempts()).isEqualTo(2);
        assertThat(policy.getPhoneRateLimitWindowSeconds()).isEqualTo(120);
    }

    @Test
    void exposesOtpWhenTwilioDisabled() {
        SpringRegistrationPolicy policy = new SpringRegistrationPolicy(properties(false));

        assertThat(policy.isExposeOtpInResponse()).isTrue();
    }

    @Test
    void hidesOtpWhenTwilioEnabled() {
        SpringRegistrationPolicy policy = new SpringRegistrationPolicy(properties(true));

        assertThat(policy.isExposeOtpInResponse()).isFalse();
    }
}
