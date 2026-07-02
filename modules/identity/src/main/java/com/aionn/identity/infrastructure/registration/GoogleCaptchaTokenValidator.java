package com.aionn.identity.infrastructure.registration;

import com.aionn.identity.application.port.out.registration.CaptchaTokenValidatorPort;
import com.aionn.identity.infrastructure.config.properties.RegistrationProperties;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "identity.registration.captcha", name = "provider", havingValue = "google")
public class GoogleCaptchaTokenValidator implements CaptchaTokenValidatorPort {

    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    private final RegistrationProperties properties;
    private final RestClient restClient;

    public GoogleCaptchaTokenValidator(RegistrationProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    @Override
    public boolean isValid(String captchaToken) {
        String secret = properties.captcha().googleSecretKey();
        if (secret == null || secret.isBlank() || captchaToken == null || captchaToken.isBlank()) {
            return false;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", secret);
        form.add("response", captchaToken);

        try {
            Map<String, Object> response = restClient.post()
                    .uri(VERIFY_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response == null) {
                return false;
            }

            Object success = response.get("success");
            return success instanceof Boolean ok && ok;
        } catch (Exception ex) {
            return false;
        }
    }
}
