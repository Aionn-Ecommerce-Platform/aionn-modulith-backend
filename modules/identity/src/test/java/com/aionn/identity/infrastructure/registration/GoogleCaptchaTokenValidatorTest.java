package com.aionn.identity.infrastructure.registration;

import com.aionn.identity.infrastructure.config.properties.RegistrationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class GoogleCaptchaTokenValidatorTest {

    private static final String SECRET = "captcha-secret";

    private RegistrationProperties propertiesWithSecret(String secret) {
        return RegistrationProperties.builder()
                .captcha(RegistrationProperties.Captcha.builder()
                        .provider("google")
                        .googleSecretKey(secret)
                        .build())
                .build();
    }

    private MockRestServiceServer bindMockServer(GoogleCaptchaTokenValidator validator) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        setField(validator, "restClient", builder.build());
        return server;
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Test
    void returnsTrueWhenVerifySucceeds() {
        GoogleCaptchaTokenValidator validator = new GoogleCaptchaTokenValidator(propertiesWithSecret(SECRET));
        MockRestServiceServer server = bindMockServer(validator);
        server.expect(requestTo(containsString("recaptcha/api/siteverify")))
                .andExpect(method(POST))
                .andRespond(withSuccess("{\"success\": true}", MediaType.APPLICATION_JSON));

        assertThat(validator.isValid("token")).isTrue();
        server.verify();
    }

    @Test
    void returnsFalseWhenVerifyReturnsSuccessFalse() {
        GoogleCaptchaTokenValidator validator = new GoogleCaptchaTokenValidator(propertiesWithSecret(SECRET));
        MockRestServiceServer server = bindMockServer(validator);
        server.expect(requestTo(containsString("siteverify")))
                .andRespond(withSuccess("{\"success\": false}", MediaType.APPLICATION_JSON));

        assertThat(validator.isValid("token")).isFalse();
    }

    @Test
    void returnsFalseWhenResponseBodyEmpty() {
        GoogleCaptchaTokenValidator validator = new GoogleCaptchaTokenValidator(propertiesWithSecret(SECRET));
        MockRestServiceServer server = bindMockServer(validator);
        server.expect(requestTo(containsString("siteverify")))
                .andRespond(withStatus(HttpStatus.OK));

        assertThat(validator.isValid("token")).isFalse();
    }

    @Test
    void returnsFalseWhenHttpError() {
        GoogleCaptchaTokenValidator validator = new GoogleCaptchaTokenValidator(propertiesWithSecret(SECRET));
        MockRestServiceServer server = bindMockServer(validator);
        server.expect(requestTo(containsString("siteverify")))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThat(validator.isValid("token")).isFalse();
    }

    @Test
    void returnsFalseWhenSecretBlank() {
        GoogleCaptchaTokenValidator validator = new GoogleCaptchaTokenValidator(propertiesWithSecret(""));
        bindMockServer(validator);

        assertThat(validator.isValid("token")).isFalse();
    }

    @Test
    void returnsFalseWhenTokenBlank() {
        GoogleCaptchaTokenValidator validator = new GoogleCaptchaTokenValidator(propertiesWithSecret(SECRET));
        bindMockServer(validator);

        assertThat(validator.isValid("  ")).isFalse();
    }
}
