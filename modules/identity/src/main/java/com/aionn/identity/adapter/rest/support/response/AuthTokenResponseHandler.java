package com.aionn.identity.adapter.rest.support.response;

import com.aionn.identity.adapter.rest.dto.auth.response.AuthTokenResponse;
import com.aionn.identity.adapter.rest.dto.auth.response.LogoutAllResponse;
import com.aionn.identity.infrastructure.config.properties.AuthCookieProperties;
import com.aionn.identity.infrastructure.config.properties.AuthProperties;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
@Component
@RequiredArgsConstructor
public class AuthTokenResponseHandler {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";

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
        long maxAgeSeconds = Duration.between(Instant.now(clock), expiresAt).getSeconds();
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
}
