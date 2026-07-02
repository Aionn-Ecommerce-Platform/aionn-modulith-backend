package com.aionn.identity.infrastructure.registration;

import com.aionn.identity.application.port.out.registration.CaptchaTokenValidatorPort;
import com.aionn.identity.infrastructure.config.properties.RegistrationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "identity.registration.captcha", name = "provider", havingValue = "mock")
public class MockCaptchaTokenValidator implements CaptchaTokenValidatorPort {

    private final RegistrationProperties properties;

    public MockCaptchaTokenValidator(RegistrationProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean isValid(String captchaToken) {
        if (captchaToken == null || captchaToken.isBlank()) {
            return false;
        }
        String expected = properties.captcha().expectedToken();
        if (expected == null || expected.isBlank()) {
            return true;
        }
        return expected.equals(captchaToken);
    }
}
