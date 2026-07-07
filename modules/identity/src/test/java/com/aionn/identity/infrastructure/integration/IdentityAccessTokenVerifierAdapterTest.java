package com.aionn.identity.infrastructure.integration;

import com.aionn.identity.application.port.out.auth.AccessTokenClaims;
import com.aionn.identity.application.port.out.auth.AccessTokenIssuerPort;
import com.aionn.identity.application.port.out.auth.AuthSessionPersistencePort;
import com.aionn.identity.application.port.out.auth.TokenBlacklistPort;
import com.aionn.identity.domain.model.AuthSession;
import com.aionn.identity.domain.valueobject.AuthSessionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityAccessTokenVerifierAdapterTest {

    private static final String USER_ID = "user-1";
    private static final String SESSION_ID = "session-1";
    private static final String JTI = "jti-1";

    @Mock
    private AccessTokenIssuerPort tokenIssuer;
    @Mock
    private AuthSessionPersistencePort authSessionPersistencePort;
    @Mock
    private TokenBlacklistPort tokenBlacklist;

    private IdentityAccessTokenVerifierAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new IdentityAccessTokenVerifierAdapter(tokenIssuer, authSessionPersistencePort, tokenBlacklist);
    }

    private static LocalDateTime utcNow() {
        return LocalDateTime.now(Clock.systemUTC());
    }

    private AuthSession session(AuthSessionStatus status, String userId, LocalDateTime expiresAt) {
        LocalDateTime now = utcNow();
        return new AuthSession(SESSION_ID, userId, "127.0.0.1", "agent", status, now, now, expiresAt);
    }

    private AccessTokenClaims claims(String userId, String sessionId, String jti) {
        return new AccessTokenClaims(userId, sessionId, jti, List.of("BUYER"));
    }

    @Test
    void returnsEmptyWhenHeaderNull() {
        assertThat(adapter.verifyAndExtractUserId(null)).isEmpty();
        verify(tokenIssuer, never()).parseClaims(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void returnsEmptyWhenHeaderNotBearer() {
        assertThat(adapter.verifyAndExtractUserId("Basic abc")).isEmpty();
    }

    @Test
    void returnsEmptyWhenClaimsCannotBeParsed() {
        when(tokenIssuer.parseClaims("tok")).thenReturn(Optional.empty());

        assertThat(adapter.verifyAndExtractUserId("Bearer tok")).isEmpty();
    }

    @Test
    void returnsEmptyWhenSessionIdMissing() {
        when(tokenIssuer.parseClaims("tok")).thenReturn(Optional.of(claims(USER_ID, null, JTI)));

        assertThat(adapter.verifyAndExtractUserId("Bearer tok")).isEmpty();
    }

    @Test
    void returnsEmptyWhenUserIdMissing() {
        when(tokenIssuer.parseClaims("tok")).thenReturn(Optional.of(claims(null, SESSION_ID, JTI)));

        assertThat(adapter.verifyAndExtractUserId("Bearer tok")).isEmpty();
    }

    @Test
    void returnsEmptyWhenTokenBlacklisted() {
        when(tokenIssuer.parseClaims("tok")).thenReturn(Optional.of(claims(USER_ID, SESSION_ID, JTI)));
        when(tokenBlacklist.isBlacklisted(JTI)).thenReturn(true);

        assertThat(adapter.verifyAndExtractUserId("Bearer tok")).isEmpty();
        verify(authSessionPersistencePort, never()).findById(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void returnsEmptyWhenSessionNotFound() {
        when(tokenIssuer.parseClaims("tok")).thenReturn(Optional.of(claims(USER_ID, SESSION_ID, JTI)));
        when(tokenBlacklist.isBlacklisted(JTI)).thenReturn(false);
        when(authSessionPersistencePort.findById(SESSION_ID)).thenReturn(Optional.empty());

        assertThat(adapter.verifyAndExtractUserId("Bearer tok")).isEmpty();
    }

    @Test
    void returnsEmptyWhenSessionRevoked() {
        when(tokenIssuer.parseClaims("tok")).thenReturn(Optional.of(claims(USER_ID, SESSION_ID, JTI)));
        when(tokenBlacklist.isBlacklisted(JTI)).thenReturn(false);
        when(authSessionPersistencePort.findById(SESSION_ID))
                .thenReturn(Optional.of(session(AuthSessionStatus.REVOKED, USER_ID, utcNow().plusDays(1))));

        assertThat(adapter.verifyAndExtractUserId("Bearer tok")).isEmpty();
    }

    @Test
    void returnsEmptyWhenSessionExpired() {
        when(tokenIssuer.parseClaims("tok")).thenReturn(Optional.of(claims(USER_ID, SESSION_ID, JTI)));
        when(tokenBlacklist.isBlacklisted(JTI)).thenReturn(false);
        when(authSessionPersistencePort.findById(SESSION_ID))
                .thenReturn(
                        Optional.of(session(AuthSessionStatus.ACTIVE, USER_ID, utcNow().minusDays(1))));

        assertThat(adapter.verifyAndExtractUserId("Bearer tok")).isEmpty();
    }

    @Test
    void returnsEmptyWhenSessionUserMismatch() {
        when(tokenIssuer.parseClaims("tok")).thenReturn(Optional.of(claims(USER_ID, SESSION_ID, JTI)));
        when(tokenBlacklist.isBlacklisted(JTI)).thenReturn(false);
        when(authSessionPersistencePort.findById(SESSION_ID))
                .thenReturn(
                        Optional.of(session(AuthSessionStatus.ACTIVE, "other-user", utcNow().plusDays(1))));

        assertThat(adapter.verifyAndExtractUserId("Bearer tok")).isEmpty();
    }

    @Test
    void returnsUserIdOnValidBearerToken() {
        when(tokenIssuer.parseClaims("tok")).thenReturn(Optional.of(claims(USER_ID, SESSION_ID, JTI)));
        when(tokenBlacklist.isBlacklisted(JTI)).thenReturn(false);
        when(authSessionPersistencePort.findById(SESSION_ID))
                .thenReturn(Optional.of(session(AuthSessionStatus.ACTIVE, USER_ID, utcNow().plusDays(1))));

        assertThat(adapter.verifyAndExtractUserId("Bearer tok")).contains(USER_ID);
    }

    @Test
    void skipsBlacklistCheckWhenJtiNull() {
        lenient().when(tokenBlacklist.isBlacklisted(org.mockito.ArgumentMatchers.any())).thenReturn(true);
        when(tokenIssuer.parseClaims("tok")).thenReturn(Optional.of(claims(USER_ID, SESSION_ID, null)));
        when(authSessionPersistencePort.findById(SESSION_ID))
                .thenReturn(Optional.of(session(AuthSessionStatus.ACTIVE, USER_ID, utcNow().plusDays(1))));

        assertThat(adapter.verifyAndExtractUserId("Bearer tok")).contains(USER_ID);
        verify(tokenBlacklist, never()).isBlacklisted(org.mockito.ArgumentMatchers.anyString());
    }
}

