package com.aionn.identity.infrastructure.auth.social.google;

import com.aionn.identity.application.port.out.social.SocialUserProfile;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.infrastructure.config.properties.SocialAuthProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Field;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RemoteGoogleSocialTokenVerifierTest {

    private static final String CLIENT_ID = "client-123";
    private static final String TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo";
    private static final long FUTURE_EXP = Instant.now().getEpochSecond() + 3600;

    private RemoteGoogleSocialTokenVerifier newVerifier(String clientId) {
        SocialAuthProperties props = new SocialAuthProperties(
                new SocialAuthProperties.Google("remote", clientId, TOKEN_INFO_URL));
        return new RemoteGoogleSocialTokenVerifier(props);
    }

    private MockRestServiceServer bindMockServer(RemoteGoogleSocialTokenVerifier verifier) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        builder.baseUrl(TOKEN_INFO_URL);
        setField(verifier, "restClient", builder.build());
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

    private String tokenInfoJson(String aud, String iss, String sub, Long exp, boolean emailVerified,
            String name, String given, String family) {
        return """
                {
                  "aud": "%s",
                  "iss": "%s",
                  "sub": "%s",
                  "exp": %d,
                  "email": "u@example.com",
                  "email_verified": %s,
                  "name": %s,
                  "given_name": %s,
                  "family_name": %s
                }
                """.formatted(aud, iss, sub, exp, emailVerified,
                jsonOrNull(name), jsonOrNull(given), jsonOrNull(family));
    }

    private String jsonOrNull(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    @Test
    void verifiesValidTokenAndReturnsProfile() {
        RemoteGoogleSocialTokenVerifier verifier = newVerifier(CLIENT_ID);
        MockRestServiceServer server = bindMockServer(verifier);
        server.expect(requestTo(containsString("id_token=id-token")))
                .andRespond(withSuccess(
                        tokenInfoJson(CLIENT_ID, "accounts.google.com", "google-sub", FUTURE_EXP, true,
                                "Full Name", null, null),
                        MediaType.APPLICATION_JSON));

        SocialUserProfile profile = verifier.verify("id-token");

        assertThat(profile.providerUserId()).isEqualTo("google-sub");
        assertThat(profile.email()).isEqualTo("u@example.com");
        assertThat(profile.displayName()).isEqualTo("Full Name");
        server.verify();
    }

    @Test
    void buildsDisplayNameFromGivenAndFamilyWhenNameBlank() {
        RemoteGoogleSocialTokenVerifier verifier = newVerifier(CLIENT_ID);
        MockRestServiceServer server = bindMockServer(verifier);
        server.expect(requestTo(containsString("id_token=")))
                .andRespond(withSuccess(
                        tokenInfoJson(CLIENT_ID, "https://accounts.google.com", "google-sub", FUTURE_EXP, true,
                                null, "Given", "Family"),
                        MediaType.APPLICATION_JSON));

        SocialUserProfile profile = verifier.verify("id-token");

        assertThat(profile.displayName()).isEqualTo("Given Family");
    }

    @Test
    void returnsNullEmailWhenEmailNotVerified() {
        RemoteGoogleSocialTokenVerifier verifier = newVerifier(CLIENT_ID);
        MockRestServiceServer server = bindMockServer(verifier);
        server.expect(requestTo(containsString("id_token=")))
                .andRespond(withSuccess(
                        tokenInfoJson(CLIENT_ID, "accounts.google.com", "google-sub", FUTURE_EXP, false,
                                "Full Name", null, null),
                        MediaType.APPLICATION_JSON));

        SocialUserProfile profile = verifier.verify("id-token");

        assertThat(profile.email()).isNull();
    }

    @Test
    void throwsOnAudienceMismatch() {
        RemoteGoogleSocialTokenVerifier verifier = newVerifier(CLIENT_ID);
        MockRestServiceServer server = bindMockServer(verifier);
        server.expect(requestTo(containsString("id_token=")))
                .andRespond(withSuccess(
                        tokenInfoJson("other-client", "accounts.google.com", "google-sub", FUTURE_EXP, true,
                                "Full Name", null, null),
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> verifier.verify("id-token")).isInstanceOf(IdentityException.class);
    }

    @Test
    void throwsOnIssuerMismatch() {
        RemoteGoogleSocialTokenVerifier verifier = newVerifier(CLIENT_ID);
        MockRestServiceServer server = bindMockServer(verifier);
        server.expect(requestTo(containsString("id_token=")))
                .andRespond(withSuccess(
                        tokenInfoJson(CLIENT_ID, "evil.example.com", "google-sub", FUTURE_EXP, true,
                                "Full Name", null, null),
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> verifier.verify("id-token")).isInstanceOf(IdentityException.class);
    }

    @Test
    void throwsWhenSubjectMissing() {
        RemoteGoogleSocialTokenVerifier verifier = newVerifier(CLIENT_ID);
        MockRestServiceServer server = bindMockServer(verifier);
        server.expect(requestTo(containsString("id_token=")))
                .andRespond(withSuccess(
                        tokenInfoJson(CLIENT_ID, "accounts.google.com", "", FUTURE_EXP, true,
                                "Full Name", null, null),
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> verifier.verify("id-token")).isInstanceOf(IdentityException.class);
    }

    @Test
    void throwsWhenTokenExpired() {
        RemoteGoogleSocialTokenVerifier verifier = newVerifier(CLIENT_ID);
        MockRestServiceServer server = bindMockServer(verifier);
        long pastExp = Instant.now().getEpochSecond() - 10;
        server.expect(requestTo(containsString("id_token=")))
                .andRespond(withSuccess(
                        tokenInfoJson(CLIENT_ID, "accounts.google.com", "google-sub", pastExp, true,
                                "Full Name", null, null),
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> verifier.verify("id-token")).isInstanceOf(IdentityException.class);
    }

    @Test
    void throwsWhenPayloadEmpty() {
        RemoteGoogleSocialTokenVerifier verifier = newVerifier(CLIENT_ID);
        MockRestServiceServer server = bindMockServer(verifier);
        server.expect(requestTo(containsString("id_token=")))
                .andRespond(withStatus(HttpStatus.OK));

        assertThatThrownBy(() -> verifier.verify("id-token")).isInstanceOf(IdentityException.class);
    }

    @Test
    void throwsWhenHttpError() {
        RemoteGoogleSocialTokenVerifier verifier = newVerifier(CLIENT_ID);
        MockRestServiceServer server = bindMockServer(verifier);
        server.expect(requestTo(containsString("id_token=")))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> verifier.verify("id-token")).isInstanceOf(IdentityException.class);
    }

    @Test
    void throwsWhenTokenBlank() {
        RemoteGoogleSocialTokenVerifier verifier = newVerifier(CLIENT_ID);
        bindMockServer(verifier);

        assertThatThrownBy(() -> verifier.verify("  ")).isInstanceOf(IdentityException.class);
    }

    @Test
    void throwsWhenClientIdNotConfigured() {
        RemoteGoogleSocialTokenVerifier verifier = newVerifier(null);
        bindMockServer(verifier);

        assertThatThrownBy(() -> verifier.verify("id-token")).isInstanceOf(IdentityException.class);
    }
}
