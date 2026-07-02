package com.aionn.identity.adapter.rest.support;

import com.aionn.identity.adapter.rest.dto.auth.response.AuthTokenResponse;
import com.aionn.identity.adapter.rest.dto.auth.response.LogoutAllResponse;
import com.aionn.identity.adapter.rest.support.response.AuthTokenResponseHandler;
import com.aionn.identity.adapter.rest.support.response.NoStoreResponseFactory;
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

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthTokenResponseHandlerTest {

    @Mock
    private AuthProperties authProperties;

    private AuthTokenResponseHandler authTokenResponseHandler;

    @BeforeEach
    void setUp() {
        AuthCookieProperties cookieProperties = new AuthCookieProperties(true, "Strict");
        authTokenResponseHandler = new AuthTokenResponseHandler(
                authProperties,
                cookieProperties,
                new NoStoreResponseFactory());
        lenient().when(authProperties.mobileClientValue()).thenReturn("mobile");
    }

    @Test
    void successKeepsRefreshTokenInBodyForMobileClient() {
        when(authProperties.mobileClientValue()).thenReturn("mobile");
        AuthTokenResponse authTokenResponse = sampleAuthTokenResponse();

        ResponseEntity<ApiResponse<AuthTokenResponse>> response =
                authTokenResponseHandler.success(authTokenResponse, "mobile", "Login successful!");

        assertEquals(200, response.getStatusCode().value());
        assertNull(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE));
        assertEquals("no-cache", response.getHeaders().getFirst(HttpHeaders.PRAGMA));
        assertNotNull(response.getBody());
        assertEquals("refresh-1", response.getBody().data().refreshToken());
        assertEquals("access-1", response.getBody().data().accessToken());
    }

    @Test
    void successMovesRefreshTokenIntoCookieForNonMobileClients() {
        when(authProperties.mobileClientValue()).thenReturn("mobile");
        AuthTokenResponse authTokenResponse = sampleAuthTokenResponse();

        ResponseEntity<ApiResponse<AuthTokenResponse>> response =
                authTokenResponseHandler.success(authTokenResponse, "web", "Login successful!");

        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("refresh_token=refresh-1"));
        assertTrue(setCookie.contains("HttpOnly"));
        assertTrue(setCookie.contains("Secure"));
        assertTrue(setCookie.contains("SameSite=Strict"));
        assertTrue(setCookie.contains("Path=/api/v1/auth"));
        assertNotNull(response.getBody());
        assertNull(response.getBody().data().refreshToken());
        assertEquals("access-1", response.getBody().data().accessToken());
    }

    @Test
    void logoutResponsesClearRefreshCookie() {
        ResponseEntity<ApiResponse<Void>> logoutResponse = authTokenResponseHandler.logoutSuccess("Logout successful");
        ResponseEntity<ApiResponse<LogoutAllResponse>> logoutAllResponse =
                authTokenResponseHandler.logoutAllSuccess(new LogoutAllResponse(3));

        String logoutCookie = logoutResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        String logoutAllCookie = logoutAllResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE);

        assertNotNull(logoutCookie);
        assertTrue(logoutCookie.contains("refresh_token="));
        assertTrue(logoutCookie.contains("Max-Age=0"));
        assertEquals("Logout successful", logoutResponse.getBody().message());

        assertNotNull(logoutAllCookie);
        assertTrue(logoutAllCookie.contains("refresh_token="));
        assertTrue(logoutAllCookie.contains("Max-Age=0"));
        assertEquals(3, logoutAllResponse.getBody().data().revokedSessions());
        assertFalse(logoutAllResponse.getHeaders().getCacheControl().isBlank());
    }

    private AuthTokenResponse sampleAuthTokenResponse() {
        LocalDateTime now = LocalDateTime.now();
        return new AuthTokenResponse(
                "user-1",
                "session-1",
                "refresh-1",
                "access-1",
                now.plusMinutes(15),
                now.plusDays(7));
    }
}
