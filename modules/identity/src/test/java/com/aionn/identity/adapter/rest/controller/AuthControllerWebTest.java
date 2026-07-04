package com.aionn.identity.adapter.rest.controller;

import com.aionn.identity.adapter.rest.dto.auth.request.LoginRequest;
import com.aionn.identity.adapter.rest.dto.auth.request.SocialAuthRequest;
import com.aionn.identity.adapter.rest.dto.auth.response.AuthSessionResponse;
import com.aionn.identity.adapter.rest.dto.auth.response.AuthTokenResponse;
import com.aionn.identity.adapter.rest.dto.auth.response.LogoutAllResponse;
import com.aionn.identity.adapter.rest.mapper.auth.AuthDtoMapper;
import com.aionn.identity.adapter.rest.exception.IdentityExceptionHandler;
import com.aionn.identity.adapter.rest.support.client.AuthClientTypeArgumentResolver;
import com.aionn.identity.adapter.rest.support.client.ClientUserAgentArgumentResolver;
import com.aionn.identity.adapter.rest.support.response.AuthTokenResponseHandler;
import com.aionn.identity.adapter.rest.support.session.CurrentAccessTokenJtiArgumentResolver;
import com.aionn.identity.adapter.rest.support.session.CurrentSessionIdArgumentResolver;
import com.aionn.identity.application.port.out.auth.AccessTokenIssuerPort;
import com.aionn.identity.adapter.rest.support.MockAuthenticationArgumentResolver;
import com.aionn.identity.adapter.rest.support.MockSecurityInterceptor;
import com.aionn.identity.application.dto.auth.command.*;
import com.aionn.identity.application.dto.auth.query.GetAuthSessionsQuery;
import com.aionn.identity.application.dto.auth.result.*;
import com.aionn.identity.application.port.in.auth.*;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.infrastructure.config.properties.AuthProperties;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import com.aionn.sharedkernel.adapter.web.support.clientip.ClientIpArgumentResolver;
import com.aionn.sharedkernel.infrastructure.web.ClientIpResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerWebTest {

        @Mock
        private LoginInputPort loginInputPort;
        @Mock
        private SocialLoginInputPort socialAuthInputPort;
        @Mock
        private RefreshTokenInputPort refreshTokenInputPort;
        @Mock
        private GetAuthSessionsQueryPort getAuthSessionsQueryPort;
        @Mock
        private RevokeSessionInputPort revokeSessionInputPort;
        @Mock
        private LogoutAllInputPort logoutAllInputPort;
        @Mock
        private LogoutInputPort logoutInputPort;
        @Mock
        private AuthDtoMapper authDtoMapper;
        @Mock
        private AuthTokenResponseHandler authTokenResponseHandler;
        @Mock
        private AuthProperties authProperties;
        @Mock
        private AccessTokenIssuerPort accessTokenIssuerPort;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                AuthController controller = new AuthController(
                                loginInputPort,
                                socialAuthInputPort,
                                refreshTokenInputPort,
                                getAuthSessionsQueryPort,
                                revokeSessionInputPort,
                                logoutAllInputPort,
                                logoutInputPort,
                                authDtoMapper,
                                authTokenResponseHandler);

                lenient().when(authProperties.clientTypeHeader()).thenReturn("X-Client-Type");

                mockMvc = MockMvcBuilders.standaloneSetup(controller)
                                .setControllerAdvice(new IdentityExceptionHandler())
                                .addInterceptors(new MockSecurityInterceptor())
                                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                                                Jackson2ObjectMapperBuilder.json().build()))
                                .setCustomArgumentResolvers(
                                                new ClientIpArgumentResolver(new ClientIpResolver()),
                                                new ClientUserAgentArgumentResolver(),
                                                new AuthClientTypeArgumentResolver(authProperties),
                                                new CurrentSessionIdArgumentResolver(),
                                                new CurrentAccessTokenJtiArgumentResolver(accessTokenIssuerPort),
                                                new MockAuthenticationArgumentResolver())
                                .build();
        }

        @Test
        void loginSuccessfullyAuthenticatesAndResolvesClientContext() throws Exception {
                LocalDateTime now = LocalDateTime.now();
                LoginResult result = new LoginResult(
                                "user-123",
                                "session-456",
                                "access-xyz",
                                "refresh-abc",
                                now.plusMinutes(15),
                                now.plusDays(7));
                AuthTokenResponse response = new AuthTokenResponse(
                                result.userId(),
                                result.sessionId(),
                                result.refreshToken(),
                                result.accessToken(),
                                result.expiresAt(),
                                result.sessionExpiresAt());

                when(authDtoMapper.toLoginCommand(any(LoginRequest.class), anyString(), eq("Mozilla/5.0")))
                                .thenReturn(new LoginCommand("user@example.com", "password123", null, "192.168.1.100",
                                                "Mozilla/5.0"));
                when(loginInputPort.execute(any())).thenReturn(result);
                when(authDtoMapper.toAuthTokenResponse(result)).thenReturn(response);
                when(authTokenResponseHandler.success(response, "web", "Login successful!"))
                                .thenReturn(ResponseEntity.ok(ApiResponse.success(response, "Login successful!")));

                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(APPLICATION_JSON)
                                .header("X-Forwarded-For", "192.168.1.100")
                                .header("User-Agent", "Mozilla/5.0")
                                .header("X-Client-Type", "web")
                                .content("""
                                                {
                                                  "identity": "user@example.com",
                                                  "password": "password123"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.statusCode").value("200"))
                                .andExpect(jsonPath("$.message").value("Login successful!"))
                                .andExpect(jsonPath("$.data.userId").value("user-123"))
                                .andExpect(jsonPath("$.data.accessToken").value("access-xyz"));

                verify(authDtoMapper).toLoginCommand(
                                eq(new LoginRequest("user@example.com", "password123", null)),
                                anyString(),
                                eq("Mozilla/5.0"));
                verify(authTokenResponseHandler).success(response, "web", "Login successful!");
        }

        @Test
        void loginWithMfaCodeIncludesItInCommand() throws Exception {
                LocalDateTime now = LocalDateTime.now();
                LoginResult result = new LoginResult("user-123", "session-456", "access-xyz", "refresh-abc",
                                now.plusMinutes(15), now.plusDays(7));
                AuthTokenResponse response = new AuthTokenResponse(result.userId(), result.sessionId(),
                                result.refreshToken(), result.accessToken(), result.expiresAt(),
                                result.sessionExpiresAt());

                when(authDtoMapper.toLoginCommand(any(LoginRequest.class), anyString(), anyString()))
                                .thenReturn(new LoginCommand("user@example.com", "password123", "123456", "192.168.1.1",
                                                "Agent"));
                when(loginInputPort.execute(any())).thenReturn(result);
                when(authDtoMapper.toAuthTokenResponse(result)).thenReturn(response);
                when(authTokenResponseHandler.success(response, "mobile", "Login successful!"))
                                .thenReturn(ResponseEntity.ok(ApiResponse.success(response, "Login successful!")));

                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(APPLICATION_JSON)
                                .header("X-Client-Type", "mobile")
                                .content("""
                                                {
                                                  "identity": "user@example.com",
                                                  "password": "password123",
                                                  "mfaCode": "123456"
                                                }
                                                """))
                                .andExpect(status().isOk());

                verify(authDtoMapper).toLoginCommand(
                                eq(new LoginRequest("user@example.com", "password123", "123456")),
                                anyString(),
                                anyString());
        }

        @Test
        void loginRejectsEmptyIdentity() throws Exception {
                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "identity": "",
                                                  "password": "password123"
                                                }
                                                """))
                                .andExpect(status().isBadRequest());

                verifyNoInteractions(loginInputPort);
        }

        @Test
        void loginRejectsEmptyPassword() throws Exception {
                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "identity": "user@example.com",
                                                  "password": ""
                                                }
                                                """))
                                .andExpect(status().isBadRequest());

                verifyNoInteractions(loginInputPort);
        }

        @Test
        void socialLoginSuccessfullyAuthenticatesWithSocialToken() throws Exception {
                LocalDateTime now = LocalDateTime.now();
                SocialLoginResult result = new SocialLoginResult("user-789", "session-999", "access-social",
                                "refresh-social", now.plusMinutes(15), now.plusDays(7), false);
                AuthTokenResponse response = new AuthTokenResponse(result.userId(), result.sessionId(),
                                result.refreshToken(), result.accessToken(), result.expiresAt(),
                                result.sessionExpiresAt());

                when(authDtoMapper.toSocialLoginCommand(any(SocialAuthRequest.class), anyString(),
                                eq("SocialApp/1.0")))
                                .thenReturn(new SocialLoginCommand("GOOGLE", "google-token-xyz", "10.0.0.1",
                                                "SocialApp/1.0"));
                when(socialAuthInputPort.execute(any())).thenReturn(result);
                when(authDtoMapper.toAuthTokenResponse(result)).thenReturn(response);
                when(authTokenResponseHandler.success(response, "mobile", "Social login successful!"))
                                .thenReturn(ResponseEntity
                                                .ok(ApiResponse.success(response, "Social login successful!")));

                mockMvc.perform(post("/api/v1/auth/social-login")
                                .contentType(APPLICATION_JSON)
                                .header("X-Forwarded-For", "10.0.0.1")
                                .header("User-Agent", "SocialApp/1.0")
                                .header("X-Client-Type", "mobile")
                                .content("""
                                                {
                                                  "provider": "GOOGLE",
                                                  "providerToken": "google-token-xyz"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.userId").value("user-789"));

                verify(socialAuthInputPort).execute(any());
        }

        @Test
        void refreshTokenFromBodySuccessfullyRefreshesTokens() throws Exception {
                LocalDateTime now = LocalDateTime.now();
                RefreshAccessTokenResult result = new RefreshAccessTokenResult("user-123", "session-456", "new-access",
                                "new-refresh",
                                now.plusMinutes(15), now.plusDays(7));
                AuthTokenResponse response = new AuthTokenResponse(result.userId(), result.sessionId(),
                                result.refreshToken(), result.accessToken(), result.expiresAt(),
                                result.sessionExpiresAt());

                when(authDtoMapper.toRefreshCommand(any(), eq(null), anyString(), anyString()))
                                .thenReturn(new RefreshTokenCommand("old-refresh-token", null, "192.168.1.1", "Agent"));
                when(refreshTokenInputPort.execute(any())).thenReturn(result);
                when(authDtoMapper.toAuthTokenResponse(result)).thenReturn(response);
                when(authTokenResponseHandler.success(response, "web", "Token refreshed successfully!"))
                                .thenReturn(ResponseEntity
                                                .ok(ApiResponse.success(response, "Token refreshed successfully!")));

                mockMvc.perform(post("/api/v1/auth/refresh")
                                .contentType(APPLICATION_JSON)
                                .header("X-Client-Type", "web")
                                .content("""
                                                {
                                                  "refreshToken": "old-refresh-token"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.accessToken").value("new-access"));

                verify(refreshTokenInputPort).execute(any());
        }

        @Test
        void refreshTokenFromCookieUsesItWhenBodyEmpty() throws Exception {
                LocalDateTime now = LocalDateTime.now();
                RefreshAccessTokenResult result = new RefreshAccessTokenResult("user-123", "session-456", "new-access",
                                "new-refresh",
                                now.plusMinutes(15), now.plusDays(7));
                AuthTokenResponse response = new AuthTokenResponse(result.userId(), result.sessionId(),
                                result.refreshToken(), result.accessToken(), result.expiresAt(),
                                result.sessionExpiresAt());

                when(authDtoMapper.toRefreshCommand(eq(null), eq("cookie-refresh-token"), anyString(), anyString()))
                                .thenReturn(new RefreshTokenCommand(null, "cookie-refresh-token", "192.168.1.1",
                                                "Agent"));
                when(refreshTokenInputPort.execute(any())).thenReturn(result);
                when(authDtoMapper.toAuthTokenResponse(result)).thenReturn(response);
                when(authTokenResponseHandler.success(response, "web", "Token refreshed successfully!"))
                                .thenReturn(ResponseEntity
                                                .ok(ApiResponse.success(response, "Token refreshed successfully!")));

                mockMvc.perform(post("/api/v1/auth/refresh")
                                .contentType(APPLICATION_JSON)
                                .header("X-Client-Type", "web")
                                .cookie(new java.io.File("").getName().equals("")
                                                ? new jakarta.servlet.http.Cookie("refresh_token",
                                                                "cookie-refresh-token")
                                                : null))
                                .andExpect(status().isOk());

                verify(authDtoMapper).toRefreshCommand(eq(null), eq("cookie-refresh-token"), anyString(), anyString());
        }

        @Test
        void getSessionsReturnsActiveSessionsForAuthenticatedUser() throws Exception {
                LocalDateTime now = LocalDateTime.now();
                AuthSessionResult session1 = new AuthSessionResult("session-1", "user-123", "ACTIVE", "192.168.1.1",
                                "Web Browser", now.minusHours(2), now.minusHours(2), now.plusDays(7));
                AuthSessionResult session2 = new AuthSessionResult("session-2", "user-123", "ACTIVE", "10.0.0.5",
                                "Mobile App", now.minusDays(1), now.minusDays(1), now.plusDays(6));
                List<AuthSessionResult> sessions = List.of(session1, session2);

                AuthSessionResponse resp1 = new AuthSessionResponse("session-1", "user-123", "ACTIVE", "192.168.1.1",
                                "Web Browser", now.minusHours(2), now.minusHours(2), now.plusDays(7));
                AuthSessionResponse resp2 = new AuthSessionResponse("session-2", "user-123", "ACTIVE", "10.0.0.5",
                                "Mobile App", now.minusDays(1), now.minusDays(1), now.plusDays(6));

                when(authDtoMapper.toGetSessionsQuery("user@example.com"))
                                .thenReturn(new GetAuthSessionsQuery("user-123"));
                when(getAuthSessionsQueryPort.execute(any())).thenReturn(sessions);
                when(authDtoMapper.toAuthSessionResponses(sessions)).thenReturn(List.of(resp1, resp2));

                mockMvc.perform(get("/api/v1/auth/sessions")
                                .with(user("user@example.com").roles("USER")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data[0].sessionId").value("session-1"))
                                .andExpect(jsonPath("$.data[1].sessionId").value("session-2"));

                verify(getAuthSessionsQueryPort).execute(any());
        }

        @Test
        void revokeSessionSuccessfullyRevokesSpecifiedSession() throws Exception {
                when(authDtoMapper.toRevokeSessionCommand("user@example.com", "session-to-revoke"))
                                .thenReturn(new RevokeSessionCommand("user-123", "session-to-revoke"));
                doNothing().when(revokeSessionInputPort).execute(any());

                mockMvc.perform(delete("/api/v1/auth/sessions/session-to-revoke")
                                .with(user("user@example.com").roles("USER")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Session revoked"));

                verify(revokeSessionInputPort).execute(any());
        }

        @Test
        void logoutSuccessfullyEndsCurrentSession() throws Exception {
                when(authDtoMapper.toLogoutCommand("user@example.com", "current-session", "jti-123"))
                                .thenReturn(new LogoutCommand("user-123", "current-session", "jti-123"));
                doNothing().when(logoutInputPort).execute(any());
                when(authTokenResponseHandler.logoutSuccess("Logout successful"))
                                .thenReturn(ResponseEntity.ok(ApiResponse.success("Logout successful")));
                when(accessTokenIssuerPort.parseClaims(any()))
                                .thenReturn(java.util.Optional
                                                .of(new com.aionn.identity.application.port.out.auth.AccessTokenClaims(
                                                                "user-123", "current-session", "jti-123",
                                                                List.of("USER"))));

                mockMvc.perform(post("/api/v1/auth/logout")
                                .with(user("user@example.com").roles("USER"))
                                .requestAttr("identity.session.id", "current-session"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Logout successful"));

                verify(logoutInputPort).execute(any());
        }

        @Test
        void logoutWhenNoSessionReturnsAlreadyLoggedOut() throws Exception {
                when(authTokenResponseHandler.logoutSuccess("Already logged out"))
                                .thenReturn(ResponseEntity.ok(ApiResponse.success("Already logged out")));

                mockMvc.perform(post("/api/v1/auth/logout")
                                .with(user("user@example.com").roles("USER")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Already logged out"));

                verifyNoInteractions(logoutInputPort);
        }

        @Test
        void logoutAllSuccessfullyRevokesAllSessions() throws Exception {
                LogoutAllResult result = new LogoutAllResult(3);
                LogoutAllResponse response = new LogoutAllResponse(3);

                when(authDtoMapper.toLogoutAllCommand("user@example.com"))
                                .thenReturn(new LogoutAllCommand("user-123"));
                when(logoutAllInputPort.execute(any())).thenReturn(result);
                when(authDtoMapper.toLogoutAllResponse(result)).thenReturn(response);
                when(authTokenResponseHandler.logoutAllSuccess(response))
                                .thenReturn(ResponseEntity.ok(ApiResponse.success(response, "All sessions revoked")));

                mockMvc.perform(post("/api/v1/auth/logout-all")
                                .with(user("user@example.com").roles("USER")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.revokedSessions").value(3));

                verify(logoutAllInputPort).execute(any());
        }

        @Test
        void loginWithInvalidCredentialsReturns401() throws Exception {
                when(authDtoMapper.toLoginCommand(any(LoginRequest.class), anyString(), anyString()))
                                .thenReturn(new LoginCommand("user@example.com", "wrong", null, "192.168.1.1",
                                                "Agent"));
                when(loginInputPort.execute(any()))
                                .thenThrow(new IdentityException(IdentityErrorCode.INVALID_CREDENTIALS));

                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "identity": "user@example.com",
                                                  "password": "wrong"
                                                }
                                                """))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.data.errorCode").value("IDENTITY_203"));
        }

        @Test
        void loginWhenUserInactiveReturns403() throws Exception {
                when(authDtoMapper.toLoginCommand(any(LoginRequest.class), anyString(), anyString()))
                                .thenReturn(new LoginCommand("user@example.com", "password", null, "192.168.1.1",
                                                "Agent"));
                when(loginInputPort.execute(any()))
                                .thenThrow(new IdentityException(IdentityErrorCode.USER_INACTIVE));

                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "identity": "user@example.com",
                                                  "password": "password"
                                                }
                                                """))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.data.errorCode").value("IDENTITY_204"));
        }

        @Test
        void loginWhenOtpRequiredReturns400() throws Exception {
                when(authDtoMapper.toLoginCommand(any(LoginRequest.class), anyString(), anyString()))
                                .thenReturn(new LoginCommand("user@example.com", "password", null, "192.168.1.1",
                                                "Agent"));
                when(loginInputPort.execute(any()))
                                .thenThrow(new IdentityException(IdentityErrorCode.OTP_REQUIRED));

                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "identity": "user@example.com",
                                                  "password": "password"
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.data.errorCode").value("IDENTITY_211"));
        }

        @Test
        void socialLoginWithUnsupportedProviderReturns400() throws Exception {
                when(authDtoMapper.toSocialLoginCommand(any(SocialAuthRequest.class), anyString(), anyString()))
                                .thenReturn(new SocialLoginCommand("UNKNOWN", "token", "127.0.0.1", "Agent"));
                when(socialAuthInputPort.execute(any()))
                                .thenThrow(new IdentityException(IdentityErrorCode.PROVIDER_NOT_SUPPORTED));

                mockMvc.perform(post("/api/v1/auth/social-login")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "provider": "UNKNOWN",
                                                  "providerToken": "token"
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.data.errorCode").value("IDENTITY_209"));
        }

        @Test
        void socialLoginWithInvalidProviderTokenReturns401() throws Exception {
                when(authDtoMapper.toSocialLoginCommand(any(SocialAuthRequest.class), anyString(), anyString()))
                                .thenReturn(new SocialLoginCommand("GOOGLE", "bad-token", "127.0.0.1", "Agent"));
                when(socialAuthInputPort.execute(any()))
                                .thenThrow(new IdentityException(IdentityErrorCode.PROVIDER_TOKEN_INVALID));

                mockMvc.perform(post("/api/v1/auth/social-login")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "provider": "GOOGLE",
                                                  "providerToken": "bad-token"
                                                }
                                                """))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.data.errorCode").value("IDENTITY_210"));
        }

        @Test
        void socialLoginRejectsBlankProvider() throws Exception {
                mockMvc.perform(post("/api/v1/auth/social-login")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "provider": "",
                                                  "providerToken": "token"
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.data.errorCode").value("VALIDATION_FAILED"));

                verifyNoInteractions(socialAuthInputPort);
        }

        @Test
        void refreshTokenWhenSessionNotFoundReturns404() throws Exception {
                when(authDtoMapper.toRefreshCommand(any(), eq(null), anyString(), anyString()))
                                .thenReturn(new RefreshTokenCommand("expired-refresh", null, "127.0.0.1", "Agent"));
                when(refreshTokenInputPort.execute(any()))
                                .thenThrow(new IdentityException(IdentityErrorCode.SESSION_NOT_FOUND));

                mockMvc.perform(post("/api/v1/auth/refresh")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "refreshToken": "expired-refresh"
                                                }
                                                """))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.data.errorCode").value("IDENTITY_205"));
        }

        @Test
        void revokeSessionWhenForbiddenReturns403() throws Exception {
                when(authDtoMapper.toRevokeSessionCommand("user@example.com", "other-user-session"))
                                .thenReturn(new RevokeSessionCommand("user-123", "other-user-session"));
                doThrow(new IdentityException(IdentityErrorCode.SESSION_FORBIDDEN))
                                .when(revokeSessionInputPort).execute(any());

                mockMvc.perform(delete("/api/v1/auth/sessions/other-user-session")
                                .with(user("user@example.com").roles("USER")))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.data.errorCode").value("IDENTITY_206"));
        }

        @Test
        void loginRejectsMalformedJson() throws Exception {
                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(APPLICATION_JSON)
                                .content("{ this-is-not-json }"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.data.errorCode").value("MALFORMED_BODY"));

                verifyNoInteractions(loginInputPort);
        }

        @Test
        void unauthorizedRequestToProtectedEndpointReturns401() throws Exception {
                mockMvc.perform(get("/api/v1/auth/sessions"))
                                .andExpect(status().isUnauthorized());
        }
}
