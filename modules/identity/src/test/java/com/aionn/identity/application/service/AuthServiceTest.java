package com.aionn.identity.application.service;

import com.aionn.identity.application.dto.auth.command.LogoutAllCommand;
import com.aionn.identity.application.dto.auth.command.LogoutCommand;
import com.aionn.identity.application.dto.auth.command.RevokeSessionCommand;
import com.aionn.identity.application.dto.auth.result.LogoutAllResult;
import com.aionn.identity.application.mapper.AuthResultMapper;
import com.aionn.identity.application.policy.AuthPolicy;
import com.aionn.identity.application.port.out.auth.AccessTokenIssuerPort;
import com.aionn.identity.application.port.out.auth.AuthSessionPersistencePort;
import com.aionn.identity.application.port.out.auth.RefreshTokenStorePort;
import com.aionn.identity.application.port.out.auth.TokenBlacklistPort;
import com.aionn.identity.application.port.out.observability.IdentityMetricsPort;
import com.aionn.identity.application.port.out.security.MfaPersistencePort;
import com.aionn.identity.application.port.out.security.PasswordHasherPort;
import com.aionn.identity.application.port.out.security.TotpManagerPort;
import com.aionn.identity.application.port.out.security.UserSecurityPort;
import com.aionn.identity.application.port.out.social.SocialLinkPersistencePort;
import com.aionn.identity.application.port.out.social.SocialTokenVerifierPort;
import com.aionn.identity.application.port.out.user.UserPersistencePort;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.model.AuthSession;
import com.aionn.identity.domain.model.IdentityUser;
import com.aionn.identity.domain.valueobject.AuthSessionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String USER_ID = "user-1";
    private static final String SESSION_ID = "session-1";

    @Mock private UserPersistencePort userPersistencePort;
    @Mock private UserSecurityPort userSecurityPort;
    @Mock private AuthSessionPersistencePort authSessionPersistencePort;
    @Mock private SocialLinkPersistencePort socialLinkPersistencePort;
    @Mock private MfaPersistencePort mfaPersistencePort;
    @Mock private PasswordHasherPort passwordHasher;
    @Mock private TotpManagerPort totpManager;
    @Mock private AccessTokenIssuerPort accessTokenIssuer;
    @Mock private SocialTokenVerifierPort socialTokenVerifier;
    @Mock private AuthPolicy authPolicy;
    @Mock private RefreshTokenStorePort refreshTokenStore;
    @Mock private AuthResultMapper authResultMapper;
    @Mock private TokenBlacklistPort tokenBlacklist;
    @Mock private IdentityMetricsPort identityMetrics;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userPersistencePort, userSecurityPort, authSessionPersistencePort,
                socialLinkPersistencePort, mfaPersistencePort, passwordHasher,
                totpManager, accessTokenIssuer, socialTokenVerifier, authPolicy,
                refreshTokenStore, authResultMapper, tokenBlacklist, identityMetrics);
    }

    private static IdentityUser activeUser() {
        return IdentityUser.createNew(USER_ID, "u@example.com", null, "user");
    }

    private static AuthSession activeSession() {
        return AuthSession.createNew(SESSION_ID, USER_ID, "127.0.0.1", "agent",
                LocalDateTime.now().plusDays(7));
    }

    @Test
    void logoutAllRevokesActiveSessionsAndReturnsCount() {
        AuthSession a = activeSession();
        AuthSession b = AuthSession.createNew("s2", USER_ID, "ip", "ua",
                LocalDateTime.now().plusDays(7));
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(authSessionPersistencePort.findByUserId(USER_ID)).thenReturn(List.of(a, b));
        when(authResultMapper.toLogoutAllResult(2)).thenReturn(new LogoutAllResult(2));

        LogoutAllResult result = authService.logoutAll(new LogoutAllCommand(USER_ID));

        assertEquals(2, result.revokedSessions());
        verify(refreshTokenStore).revokeBySessionId(SESSION_ID);
        verify(refreshTokenStore).revokeBySessionId("s2");
        verify(authSessionPersistencePort).saveAll(List.of(a, b));
    }

    @Test
    void logoutAllForUnknownUserThrows() {
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(IdentityException.class,
                () -> authService.logoutAll(new LogoutAllCommand(USER_ID)));
    }

    @Test
    void logoutRevokesSessionAndBlacklistsAccessToken() {
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(authSessionPersistencePort.findById(SESSION_ID))
                .thenReturn(Optional.of(activeSession()));
        when(authPolicy.getAccessTokenExpiryMinutes()).thenReturn(15);

        authService.logout(new LogoutCommand(USER_ID, SESSION_ID, "jti-1"));

        verify(refreshTokenStore).revokeBySessionId(SESSION_ID);
        verify(tokenBlacklist).blacklist("jti-1", 15 * 60L);
    }

    @Test
    void logoutSkipsBlacklistWhenJtiBlank() {
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(authSessionPersistencePort.findById(SESSION_ID))
                .thenReturn(Optional.of(activeSession()));

        authService.logout(new LogoutCommand(USER_ID, SESSION_ID, ""));

        verify(refreshTokenStore).revokeBySessionId(SESSION_ID);
        verify(tokenBlacklist, never()).blacklist(anyString(), anyLong());
    }

    @Test
    void revokeSessionMovesSessionToRevoked() {
        AuthSession session = activeSession();
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(authSessionPersistencePort.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(authSessionPersistencePort.save(any(AuthSession.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        authService.revokeSession(new RevokeSessionCommand(USER_ID, SESSION_ID));

        assertEquals(AuthSessionStatus.REVOKED, session.getStatus());
        verify(refreshTokenStore).revokeBySessionId(SESSION_ID);
    }
}
