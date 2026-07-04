package com.aionn.identity.infrastructure.registration;

import com.aionn.identity.application.port.out.registration.CaptchaTokenValidatorPort;
import com.aionn.identity.infrastructure.config.properties.RegistrationProperties;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.http.MediaType;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "identity.registration.captcha", name = "provider", havingValue = "google")
public class GoogleCaptchaTokenValidator implements CaptchaTokenValidatorPort {

    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    private final RegistrationProperties properties;
    private final RestClient restClient;

    public GoogleCaptchaTokenValidator(RegistrationProperties properties) {
        this.properties = properties;
        // Explicit timeouts so a slow / hung siteverify call cannot pin the
        // registration thread indefinitely.
        var settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(CONNECT_TIMEOUT)
                .withReadTimeout(READ_TIMEOUT);
        this.restClient = RestClient.builder()
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
                .build();
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
            // Log so an infra outage (bad secret, network) is distinguishable
            // from a legitimate invalid token; still fail closed.
            log.warn("Captcha verification failed: {}", ex.getMessage());
            return false;
        }
    }
}
