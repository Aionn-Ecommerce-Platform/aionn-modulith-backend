package com.aionn.identity.infrastructure.registration;

import com.aionn.identity.infrastructure.config.properties.RegistrationProperties;
import com.aionn.identity.infrastructure.config.properties.RegistrationProperties.Captcha;
import com.aionn.identity.infrastructure.config.properties.RegistrationProperties.RateLimit;
import com.aionn.identity.infrastructure.config.properties.RegistrationProperties.Twilio;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockCaptchaTokenValidatorTest {

    private MockCaptchaTokenValidator validatorWithExpectedToken(String expectedToken) {
        RegistrationProperties properties = new RegistrationProperties(
                5, 60, 300, 30, 30L, "+84",
                new RateLimit(3, 300, 1, 60),
                new Captcha("mock", expectedToken, "", ""),
                new Twilio(false, "", "", ""));
        return new MockCaptchaTokenValidator(properties);
    }

    @Test
    void rejectsNullOrBlankToken() {
        MockCaptchaTokenValidator validator = validatorWithExpectedToken("secret");

        assertThat(validator.isValid(null)).isFalse();
        assertThat(validator.isValid("   ")).isFalse();
    }

    @Test
    void acceptsAnyTokenWhenNoExpectedTokenConfigured() {
        MockCaptchaTokenValidator validator = validatorWithExpectedToken("");

        assertThat(validator.isValid("whatever")).isTrue();
    }

    @Test
    void acceptsMatchingExpectedToken() {
        MockCaptchaTokenValidator validator = validatorWithExpectedToken("secret");

        assertThat(validator.isValid("secret")).isTrue();
    }

    @Test
    void rejectsNonMatchingToken() {
        MockCaptchaTokenValidator validator = validatorWithExpectedToken("secret");

        assertThat(validator.isValid("wrong")).isFalse();
    }
}
