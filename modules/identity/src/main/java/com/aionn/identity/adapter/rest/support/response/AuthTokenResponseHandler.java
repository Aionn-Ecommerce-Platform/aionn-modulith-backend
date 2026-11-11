package com.aionn.identity.adapter.rest.support.response;

import com.aionn.identity.adapter.rest.dto.auth.response.AuthTokenResponse;
import com.aionn.identity.adapter.rest.dto.auth.response.LogoutAllResponse;
import com.aionn.identity.infrastructure.config.properties.AuthCookieProperties;
import com.aionn.identity.infrastructure.config.properties.AuthProperties;
import com.aionn.platform.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
@Component
@RequiredArgsConstructor
public class AuthTokenResponseHandler {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AuthProperties authProperties;
    private final AuthCookieProperties cookieProperties;
    private final NoStoreResponseFactory noStoreResponseFactory;
    private final Clock clock;

    public ResponseEntity<ApiResponse<AuthTokenResponse>> success(
            AuthTokenResponse response,
            String clientType,
            String message) {

        if (isMobileClient(clientType)) {
            return noStoreResponseFactory.ok(ApiResponse.success(response, message));
        }

        AuthTokenResponse webResponse = new AuthTokenResponse(
                response.userId(),
                response.sessionId(),
                null,
                response.accessToken(),
                response.expiresAt(),
                response.sessionExpiresAt());

        HttpHeaders cookieHeaders = new HttpHeaders();
        if (response.refreshToken() != null) {
            cookieHeaders.add(HttpHeaders.SET_COOKIE,
                    buildRefreshCookie(response.refreshToken(), response.sessionExpiresAt()).toString());
            cookieHeaders.add(HttpHeaders.SET_COOKIE,
                    buildCsrfCookie(response.sessionExpiresAt()).toString());
        }
        return noStoreResponseFactory.ok(ApiResponse.success(webResponse, message), cookieHeaders);
    }

    public ResponseEntity<ApiResponse<Void>> logoutSuccess(String message) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString());
        return noStoreResponseFactory.ok(ApiResponse.success(message), headers);
    }

    public ResponseEntity<ApiResponse<LogoutAllResponse>> logoutAllSuccess(LogoutAllResponse response) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString());
        return noStoreResponseFactory.ok(ApiResponse.success(response, "All sessions revoked"), headers);
    }

    private boolean isMobileClient(String clientType) {
        if (clientType == null || clientType.isBlank()) {
            return false;
        }
        return authProperties.mobileClientValue().equalsIgnoreCase(clientType.trim());
    }

    private ResponseCookie buildRefreshCookie(String refreshToken, Instant expiresAt) {
        long maxAgeSeconds = Duration.between(clock.instant(), expiresAt).getSeconds();
        if (maxAgeSeconds < 0) {
            maxAgeSeconds = 0;
        }
        return ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path(cookieProperties.refreshPath())
                .maxAge(maxAgeSeconds)
                .build();
    }

    private ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path(cookieProperties.refreshPath())
                .maxAge(0)
                .build();
    }

    /**
     * Double-submit CSRF token. The value is random, readable by the SPA and echoed back in
     * {@code X-XSRF-TOKEN}; the server never stores it.
     */
    private ResponseCookie buildCsrfCookie(Instant expiresAt) {
        long maxAgeSeconds = Math.max(0, Duration.between(clock.instant(), expiresAt).getSeconds());
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        return ResponseCookie.from(CSRF_COOKIE_NAME, token)
                .httpOnly(false)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }
}
