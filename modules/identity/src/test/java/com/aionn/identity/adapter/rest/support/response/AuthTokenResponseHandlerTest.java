package com.aionn.identity.adapter.rest.support.response;

import com.aionn.identity.adapter.rest.dto.auth.response.AuthTokenResponse;
import com.aionn.identity.adapter.rest.dto.auth.response.LogoutAllResponse;
import com.aionn.identity.infrastructure.config.properties.AuthCookieProperties;
import com.aionn.identity.infrastructure.config.properties.AuthProperties;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.Duration;

import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AuthTokenResponseHandlerTest {

    @Mock
    private AuthProperties authProperties;

    private AuthTokenResponseHandler authTokenResponseHandler;

    @BeforeEach
    void setUp() {
        AuthCookieProperties cookieProperties = new AuthCookieProperties(true, "Strict", "/api/v1/auth");
        NoStoreResponseFactory noStoreResponseFactory = new NoStoreResponseFactory();
        authTokenResponseHandler = new AuthTokenResponseHandler(
                authProperties, cookieProperties, noStoreResponseFactory);
    }

    @Test
    void successKeepsRefreshTokenInBodyForMobileClient() {
        when(authProperties.mobileClientValue()).thenReturn("mobile");
        AuthTokenResponse authTokenResponse = sampleAuthTokenResponse();

        ResponseEntity<ApiResponse<AuthTokenResponse>> response = authTokenResponseHandler.success(authTokenResponse,
                "mobile", "Login successful!");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).isNull();
        assertThat(response.getHeaders().getFirst(HttpHeaders.PRAGMA)).isEqualTo("no-cache");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().refreshToken()).isEqualTo("refresh-1");
        assertThat(response.getBody().data().accessToken()).isEqualTo("access-1");
    }

    @Test
    void successMovesRefreshTokenIntoCookieForNonMobileClients() {
        when(authProperties.mobileClientValue()).thenReturn("mobile");
        AuthTokenResponse authTokenResponse = sampleAuthTokenResponse();

        ResponseEntity<ApiResponse<AuthTokenResponse>> response = authTokenResponseHandler.success(authTokenResponse,
                "web", "Login successful!");

        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotNull();
        assertThat(setCookie.contains("refresh_token=refresh-1")).isTrue();
        assertThat(setCookie.contains("HttpOnly")).isTrue();
        assertThat(setCookie.contains("Secure")).isTrue();
        assertThat(setCookie.contains("SameSite=Strict")).isTrue();
        assertThat(setCookie.contains("Path=/api/v1/auth")).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().refreshToken()).isNull();
        assertThat(response.getBody().data().accessToken()).isEqualTo("access-1");
    }

    @Test
    void logoutResponsesClearRefreshCookie() {
        ResponseEntity<ApiResponse<Void>> logoutResponse = authTokenResponseHandler.logoutSuccess("Logout successful");
        ResponseEntity<ApiResponse<LogoutAllResponse>> logoutAllResponse = authTokenResponseHandler
                .logoutAllSuccess(new LogoutAllResponse(3));

        String logoutCookie = logoutResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        String logoutAllCookie = logoutAllResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE);

        assertThat(logoutCookie).isNotNull();
        assertThat(logoutCookie.contains("refresh_token=")).isTrue();
        assertThat(logoutCookie.contains("Max-Age=0")).isTrue();
        assertThat(logoutResponse.getBody().message()).isEqualTo("Logout successful");

        assertThat(logoutAllCookie).isNotNull();
        assertThat(logoutAllCookie.contains("refresh_token=")).isTrue();
        assertThat(logoutAllCookie.contains("Max-Age=0")).isTrue();
        assertThat(logoutAllResponse.getBody().data().revokedSessions()).isEqualTo(3);
        assertThat(logoutAllResponse.getHeaders().getCacheControl().isBlank()).isFalse();
    }

    private AuthTokenResponse sampleAuthTokenResponse() {
        Instant now = Instant.now();
        return new AuthTokenResponse(
                "user-1",
                "session-1",
                "refresh-1",
                "access-1",
                now.plus(Duration.ofMinutes(15)),
                now.plus(Duration.ofDays(7)));
    }
}
