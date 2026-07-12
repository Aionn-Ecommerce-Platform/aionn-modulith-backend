package com.aionn.identity.application.service;

import com.aionn.identity.application.dto.auth.command.LinkSocialCommand;
import com.aionn.identity.application.dto.auth.command.LoginCommand;
import com.aionn.identity.application.dto.auth.command.LogoutAllCommand;
import com.aionn.identity.application.dto.auth.command.LogoutCommand;
import com.aionn.identity.application.dto.auth.command.RefreshTokenCommand;
import com.aionn.identity.application.dto.auth.command.RevokeSessionCommand;
import com.aionn.identity.application.dto.auth.command.SocialLoginCommand;
import com.aionn.identity.application.dto.auth.command.UnlinkSocialCommand;
import com.aionn.identity.application.dto.auth.result.LoginResult;
import com.aionn.identity.application.dto.auth.result.LogoutAllResult;
import com.aionn.identity.application.dto.auth.result.RefreshAccessTokenResult;
import com.aionn.identity.application.dto.auth.result.SocialLoginResult;
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
import com.aionn.identity.application.port.out.social.SocialUserProfile;
import com.aionn.identity.application.port.out.user.UserPersistencePort;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.model.AuthSession;
import com.aionn.identity.domain.model.IdentityUser;
import com.aionn.identity.domain.model.SocialLink;
import com.aionn.identity.domain.valueobject.AuthProvider;
import com.aionn.identity.domain.valueobject.AuthSessionStatus;
import com.aionn.identity.domain.valueobject.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String USER_ID = "user-1";
    private static final String SESSION_ID = "session-1";

    @Mock
    private UserPersistencePort userPersistencePort;
    @Mock
    private UserSecurityPort userSecurityPort;
    @Mock
    private AuthSessionPersistencePort authSessionPersistencePort;
    @Mock
    private SocialLinkPersistencePort socialLinkPersistencePort;
    @Mock
    private MfaPersistencePort mfaPersistencePort;
    @Mock
    private PasswordHasherPort passwordHasher;
    @Mock
    private TotpManagerPort totpManager;
    @Mock
    private AccessTokenIssuerPort accessTokenIssuer;
    @Mock
    private SocialTokenVerifierPort socialTokenVerifier;
    @Mock
    private AuthPolicy authPolicy;
    @Mock
    private RefreshTokenStorePort refreshTokenStore;
    @Mock
    private AuthResultMapper authResultMapper;
    @Mock
    private TokenBlacklistPort tokenBlacklist;
    @Mock
    private IdentityMetricsPort identityMetrics;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userPersistencePort, userSecurityPort, authSessionPersistencePort,
                socialLinkPersistencePort, mfaPersistencePort, passwordHasher,
                totpManager, accessTokenIssuer, socialTokenVerifier, authPolicy,
                refreshTokenStore, authResultMapper, tokenBlacklist, identityMetrics,
                Clock.systemUTC());
    }

    private static AuthSession activeSession() {
        return AuthSession.createNew(SESSION_ID, USER_ID, "127.0.0.1", "agent",
                Instant.now().plus(Duration.ofDays(7)));
    }

    @Test
    void logoutAllRevokesActiveSessionsAndReturnsCount() {
        AuthSession a = activeSession();
        AuthSession b = AuthSession.createNew("s2", USER_ID, "ip", "ua",
                Instant.now().plus(Duration.ofDays(7)));
        when(userPersistencePort.existsById(USER_ID)).thenReturn(true);
        when(authSessionPersistencePort.findByUserId(USER_ID)).thenReturn(List.of(a, b));
        when(authResultMapper.toLogoutAllResult(2)).thenReturn(new LogoutAllResult(2));

        LogoutAllResult result = authService.logoutAll(new LogoutAllCommand(USER_ID));

        assertThat(result.revokedSessions()).isEqualTo(2);
        verify(refreshTokenStore).revokeBySessionId(SESSION_ID);
        verify(refreshTokenStore).revokeBySessionId("s2");
        verify(authSessionPersistencePort).saveAll(List.of(a, b));
    }

    @Test
    void logoutAllForUnknownUserThrows() {
        when(userPersistencePort.existsById(USER_ID)).thenReturn(false);

        assertThrows(IdentityException.class,
                () -> authService.logoutAll(new LogoutAllCommand(USER_ID)));
    }

    @Test
    void logoutRevokesSessionAndBlacklistsAccessToken() {
        when(userPersistencePort.existsById(USER_ID)).thenReturn(true);
        when(authSessionPersistencePort.findById(SESSION_ID))
                .thenReturn(Optional.of(activeSession()));
        when(authPolicy.getAccessTokenExpiryMinutes()).thenReturn(15);

        authService.logout(new LogoutCommand(USER_ID, SESSION_ID, "jti-1"));

        verify(refreshTokenStore).revokeBySessionId(SESSION_ID);
        verify(tokenBlacklist).blacklist("jti-1", Duration.ofMinutes(15));
    }

    @Test
    void logoutSkipsBlacklistWhenJtiBlank() {
        when(userPersistencePort.existsById(USER_ID)).thenReturn(true);
        when(authSessionPersistencePort.findById(SESSION_ID))
                .thenReturn(Optional.of(activeSession()));

        authService.logout(new LogoutCommand(USER_ID, SESSION_ID, ""));

        verify(refreshTokenStore).revokeBySessionId(SESSION_ID);
        verify(tokenBlacklist, never()).blacklist(anyString(), any(Duration.class));
    }

    @Test
    void revokeSessionMovesSessionToRevoked() {
        AuthSession session = activeSession();
        when(userPersistencePort.existsById(USER_ID)).thenReturn(true);
        when(authSessionPersistencePort.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(authSessionPersistencePort.save(any(AuthSession.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        authService.revokeSession(new RevokeSessionCommand(USER_ID, SESSION_ID));

        assertThat(session.getStatus()).isEqualTo(AuthSessionStatus.REVOKED);
        verify(refreshTokenStore).revokeBySessionId(SESSION_ID);
    }

    private static AuthSession expiredSession() {
        return AuthSession.createNew(SESSION_ID, USER_ID, "127.0.0.1", "agent",
                Instant.now(Clock.systemUTC()).minus(Duration.ofDays(1)));
    }

    private static IdentityUser activeUser() {
        return IdentityUser.createNew(USER_ID, "u@example.com", null, "user");
    }

    private static UserSecurityPort.UserSecurityData security(boolean mfaEnabled, String mfaSecret) {
        return new UserSecurityPort.UserSecurityData(
                USER_ID, "hash", UserStatus.ACTIVE, mfaEnabled, mfaSecret, null, 0);
    }

    private static void assertErrorCode(IdentityErrorCode expected, Executable executable) {
        IdentityException ex = assertThrows(IdentityException.class, executable);
        assertThat(ex.getErrorCode()).isEqualTo(expected.getCode());
    }

    private void stubTokenIssuance() {
        when(authSessionPersistencePort.save(any(AuthSession.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(accessTokenIssuer.issueAccessToken(anyString(), anyString(), any(Instant.class), anySet()))
                .thenReturn("access-token");
        when(accessTokenIssuer.extractExpiry("access-token")).thenReturn(Optional.of(Instant.now()));
    }

    @Test
    void loginSucceedsWhenMfaDisabled() {
        LoginResult expected = new LoginResult(USER_ID, SESSION_ID, "access-token", "refresh",
                Instant.now(), Instant.now().plus(Duration.ofDays(7)));
        when(userSecurityPort.findByIdentity("u@example.com")).thenReturn(Optional.of(security(false, null)));
        when(passwordHasher.matches("pw", "hash")).thenReturn(true);
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(userSecurityPort.findById(USER_ID)).thenReturn(Optional.of(security(false, null)));
        when(authPolicy.getSessionExpiresDays()).thenReturn(7L);
        stubTokenIssuance();
        when(authResultMapper.toLoginResult(any(AuthSession.class), eq("access-token"), anyString(),
                any(Instant.class))).thenReturn(expected);

        LoginResult result = authService.login(
                new LoginCommand("u@example.com", "pw", null, "127.0.0.1", "agent"));

        assertThat(result).isSameAs(expected);
        verify(userSecurityPort).resetFailedLoginAttempts(USER_ID);
        verify(identityMetrics).loginAttempt("success");
        verify(identityMetrics).sessionLifecycle("created");
        verify(refreshTokenStore).store(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void loginRejectsUnknownIdentity() {
        when(userSecurityPort.findByIdentity("nobody")).thenReturn(Optional.empty());

        assertErrorCode(IdentityErrorCode.INVALID_CREDENTIALS,
                () -> authService.login(new LoginCommand("nobody", "pw", null, "ip", "ua")));
    }

    @Test
    void loginRejectsInactiveUser() {
        when(userSecurityPort.findByIdentity("u@example.com")).thenReturn(Optional.of(
                new UserSecurityPort.UserSecurityData(
                        USER_ID, "hash", UserStatus.SUSPENDED, false, null, null, 0)));

        assertErrorCode(IdentityErrorCode.USER_INACTIVE,
                () -> authService.login(new LoginCommand("u@example.com", "pw", null, "ip", "ua")));
    }

    @Test
    void loginRejectsLockedAccount() {
        when(userSecurityPort.findByIdentity("u@example.com")).thenReturn(Optional.of(
                new UserSecurityPort.UserSecurityData(
                        USER_ID, "hash", UserStatus.ACTIVE, false, null,
                        Instant.now(Clock.systemUTC()).plus(Duration.ofDays(1)), 0)));

        assertErrorCode(IdentityErrorCode.USER_INACTIVE,
                () -> authService.login(new LoginCommand("u@example.com", "pw", null, "ip", "ua")));
    }

    @Test
    void loginWithWrongPasswordRecordsFailedAttempt() {
        when(userSecurityPort.findByIdentity("u@example.com")).thenReturn(Optional.of(security(false, null)));
        when(passwordHasher.matches("bad", "hash")).thenReturn(false);
        when(authPolicy.getMaxFailedLoginAttempts()).thenReturn(5);

        assertErrorCode(IdentityErrorCode.INVALID_CREDENTIALS,
                () -> authService.login(new LoginCommand("u@example.com", "bad", null, "ip", "ua")));

        verify(userSecurityPort).recordFailedLoginAttempt(USER_ID, 1, null);
        verify(identityMetrics).loginAttempt("failed");
    }

    @Test
    void loginWithNullStoredHashRejects() {
        when(userSecurityPort.findByIdentity("u@example.com")).thenReturn(Optional.of(
                new UserSecurityPort.UserSecurityData(
                        USER_ID, null, UserStatus.ACTIVE, false, null, null, 0)));
        when(authPolicy.getMaxFailedLoginAttempts()).thenReturn(5);

        assertErrorCode(IdentityErrorCode.INVALID_CREDENTIALS,
                () -> authService.login(new LoginCommand("u@example.com", "pw", null, "ip", "ua")));

        verify(userSecurityPort).recordFailedLoginAttempt(USER_ID, 1, null);
    }

    @Test
    void loginWrongPasswordLocksAccountWhenThresholdReached() {
        when(userSecurityPort.findByIdentity("u@example.com")).thenReturn(Optional.of(
                new UserSecurityPort.UserSecurityData(
                        USER_ID, "hash", UserStatus.ACTIVE, false, null, null, 4)));
        when(passwordHasher.matches("bad", "hash")).thenReturn(false);
        when(authPolicy.getMaxFailedLoginAttempts()).thenReturn(5);
        when(authPolicy.getLockoutMinutes()).thenReturn(15);

        assertErrorCode(IdentityErrorCode.INVALID_CREDENTIALS,
                () -> authService.login(new LoginCommand("u@example.com", "bad", null, "ip", "ua")));

        verify(userSecurityPort).recordFailedLoginAttempt(eq(USER_ID), eq(5), any(Instant.class));
    }

    @Test
    void loginRequiresMfaCodeWhenMfaEnabled() {
        when(userSecurityPort.findByIdentity("u@example.com")).thenReturn(Optional.of(security(true, "secret")));
        when(passwordHasher.matches("pw", "hash")).thenReturn(true);
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(userSecurityPort.findById(USER_ID)).thenReturn(Optional.of(security(true, "secret")));
        when(authPolicy.getMaxFailedLoginAttempts()).thenReturn(5);

        assertErrorCode(IdentityErrorCode.OTP_REQUIRED,
                () -> authService.login(new LoginCommand("u@example.com", "pw", null, "ip", "ua")));

        verify(userSecurityPort).recordFailedLoginAttempt(USER_ID, 1, null);
    }

    @Test
    void loginRejectsInvalidMfaCode() {
        when(userSecurityPort.findByIdentity("u@example.com")).thenReturn(Optional.of(security(true, "secret")));
        when(passwordHasher.matches("pw", "hash")).thenReturn(true);
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(userSecurityPort.findById(USER_ID)).thenReturn(Optional.of(security(true, "secret")));
        when(mfaPersistencePort.findActiveBackupCodes(USER_ID)).thenReturn(List.of());
        when(totpManager.verifyCode("secret", "000000")).thenReturn(false);
        when(authPolicy.getMaxFailedLoginAttempts()).thenReturn(5);

        assertErrorCode(IdentityErrorCode.OTP_INVALID,
                () -> authService.login(new LoginCommand("u@example.com", "pw", "000000", "ip", "ua")));
    }

    @Test
    void loginSucceedsWithValidTotpCode() {
        LoginResult expected = new LoginResult(USER_ID, SESSION_ID, "access-token", "refresh",
                Instant.now(), Instant.now().plus(Duration.ofDays(7)));
        when(userSecurityPort.findByIdentity("u@example.com")).thenReturn(Optional.of(security(true, "secret")));
        when(passwordHasher.matches("pw", "hash")).thenReturn(true);
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(userSecurityPort.findById(USER_ID)).thenReturn(Optional.of(security(true, "secret")));
        when(mfaPersistencePort.findActiveBackupCodes(USER_ID)).thenReturn(List.of());
        when(totpManager.verifyCode("secret", "123456")).thenReturn(true);
        when(authPolicy.getSessionExpiresDays()).thenReturn(7L);
        stubTokenIssuance();
        when(authResultMapper.toLoginResult(any(AuthSession.class), eq("access-token"), anyString(),
                any(Instant.class))).thenReturn(expected);

        LoginResult result = authService.login(
                new LoginCommand("u@example.com", "pw", "123456", "ip", "ua"));

        assertThat(result).isSameAs(expected);
        verify(userSecurityPort).resetFailedLoginAttempts(USER_ID);
    }

    @Test
    void loginSucceedsWithValidBackupCode() {
        LoginResult expected = new LoginResult(USER_ID, SESSION_ID, "access-token", "refresh",
                Instant.now(), Instant.now().plus(Duration.ofDays(7)));
        when(userSecurityPort.findByIdentity("u@example.com")).thenReturn(Optional.of(security(true, "secret")));
        when(passwordHasher.matches("pw", "hash")).thenReturn(true);
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(userSecurityPort.findById(USER_ID)).thenReturn(Optional.of(security(true, "secret")));
        when(mfaPersistencePort.findActiveBackupCodes(USER_ID)).thenReturn(
                List.of(new MfaPersistencePort.BackupCodeData("bc-1", "code-hash")));
        when(passwordHasher.matches("backup", "code-hash")).thenReturn(true);
        when(mfaPersistencePort.markBackupCodeUsed(eq("bc-1"), any(Instant.class))).thenReturn(true);
        when(authPolicy.getSessionExpiresDays()).thenReturn(7L);
        stubTokenIssuance();
        when(authResultMapper.toLoginResult(any(AuthSession.class), eq("access-token"), anyString(),
                any(Instant.class))).thenReturn(expected);

        LoginResult result = authService.login(
                new LoginCommand("u@example.com", "pw", "backup", "ip", "ua"));

        assertThat(result).isSameAs(expected);
        verify(mfaPersistencePort).markBackupCodeUsed(eq("bc-1"), any(Instant.class));
    }

    @Test
    void socialLoginWithExistingLinkLogsInUser() {
        SocialLoginResult expected = new SocialLoginResult(USER_ID, SESSION_ID, "access-token", "refresh",
                Instant.now(), Instant.now().plus(Duration.ofDays(7)), false);
        when(socialTokenVerifier.verifyAndExtract(AuthProvider.GOOGLE, "token"))
                .thenReturn(new SocialUserProfile("puid", "s@example.com", "Social User"));
        when(socialLinkPersistencePort.findByProviderAndProviderUserId(AuthProvider.GOOGLE, "puid"))
                .thenReturn(Optional.of(SocialLink.createNew("sl-1", USER_ID, AuthProvider.GOOGLE, "puid")));
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(authPolicy.getSessionExpiresDays()).thenReturn(7L);
        stubTokenIssuance();
        when(authResultMapper.toSocialLoginResult(any(AuthSession.class), eq("access-token"), anyString(),
                any(Instant.class), eq(false))).thenReturn(expected);

        SocialLoginResult result = authService.socialLogin(
                new SocialLoginCommand("google", "token", "ip", "ua"));

        assertThat(result).isSameAs(expected);
        verify(identityMetrics).socialAuth("GOOGLE", "success");
    }

    @Test
    void socialLoginCreatesNewUserWhenNoLinkExists() {
        SocialLoginResult expected = new SocialLoginResult("new-id", SESSION_ID, "access-token", "refresh",
                Instant.now(), Instant.now().plus(Duration.ofDays(7)), true);
        when(socialTokenVerifier.verifyAndExtract(AuthProvider.GOOGLE, "token"))
                .thenReturn(new SocialUserProfile("puid", "new@example.com", "New User"));
        when(socialLinkPersistencePort.findByProviderAndProviderUserId(AuthProvider.GOOGLE, "puid"))
                .thenReturn(Optional.empty());
        when(userPersistencePort.findByIdentity("new@example.com")).thenReturn(Optional.empty());
        when(userPersistencePort.existsByUsername(anyString())).thenReturn(false);
        when(userPersistencePort.save(any(IdentityUser.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userPersistencePort.findById(anyString())).thenReturn(Optional.empty());
        when(authPolicy.getSessionExpiresDays()).thenReturn(7L);
        stubTokenIssuance();
        when(authResultMapper.toSocialLoginResult(any(AuthSession.class), eq("access-token"), anyString(),
                any(Instant.class), eq(true))).thenReturn(expected);

        SocialLoginResult result = authService.socialLogin(
                new SocialLoginCommand("google", "token", "ip", "ua"));

        assertThat(result).isSameAs(expected);
        verify(userPersistencePort).save(any(IdentityUser.class));
        verify(socialLinkPersistencePort).save(any(SocialLink.class), anyString());
    }

    @Test
    void socialLoginThrowsWhenLinkedUserMissing() {
        when(socialTokenVerifier.verifyAndExtract(AuthProvider.GOOGLE, "token"))
                .thenReturn(new SocialUserProfile("puid", "s@example.com", "Social User"));
        when(socialLinkPersistencePort.findByProviderAndProviderUserId(AuthProvider.GOOGLE, "puid"))
                .thenReturn(Optional.of(SocialLink.createNew("sl-1", USER_ID, AuthProvider.GOOGLE, "puid")));
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.empty());

        assertErrorCode(IdentityErrorCode.USER_NOT_FOUND,
                () -> authService.socialLogin(new SocialLoginCommand("google", "token", "ip", "ua")));
    }

    @Test
    void socialLoginRejectsInactiveUser() {
        IdentityUser banned = activeUser();
        banned.ban();
        when(socialTokenVerifier.verifyAndExtract(AuthProvider.GOOGLE, "token"))
                .thenReturn(new SocialUserProfile("puid", "s@example.com", "Social User"));
        when(socialLinkPersistencePort.findByProviderAndProviderUserId(AuthProvider.GOOGLE, "puid"))
                .thenReturn(Optional.of(SocialLink.createNew("sl-1", USER_ID, AuthProvider.GOOGLE, "puid")));
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(banned));

        assertErrorCode(IdentityErrorCode.USER_INACTIVE,
                () -> authService.socialLogin(new SocialLoginCommand("google", "token", "ip", "ua")));
    }

    @Test
    void socialLoginRejectsUnsupportedProvider() {
        assertErrorCode(IdentityErrorCode.PROVIDER_NOT_SUPPORTED,
                () -> authService.socialLogin(new SocialLoginCommand("facebook", "token", "ip", "ua")));
    }

    @Test
    void listSessionsReturnsSessionsForActiveUser() {
        AuthSession session = activeSession();
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(authSessionPersistencePort.findByUserId(USER_ID)).thenReturn(List.of(session));

        List<AuthSession> sessions = authService.listSessions(USER_ID);

        assertThat(sessions).hasSize(1);
    }

    @Test
    void listSessionsThrowsWhenUserMissing() {
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.empty());

        assertErrorCode(IdentityErrorCode.USER_NOT_FOUND, () -> authService.listSessions(USER_ID));
    }

    @Test
    void listSessionsThrowsWhenUserInactive() {
        IdentityUser banned = activeUser();
        banned.ban();
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(banned));

        assertErrorCode(IdentityErrorCode.USER_INACTIVE, () -> authService.listSessions(USER_ID));
    }

    @Test
    void linkSocialCreatesLink() {
        SocialLink saved = SocialLink.createNew("sl-1", USER_ID, AuthProvider.GOOGLE, "puid");
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(socialTokenVerifier.verifyAndExtract(AuthProvider.GOOGLE, "token"))
                .thenReturn(new SocialUserProfile("puid", "s@example.com", "Social User"));
        when(socialLinkPersistencePort.existsByProviderAndProviderUserId(AuthProvider.GOOGLE, "puid"))
                .thenReturn(false);
        when(socialLinkPersistencePort.findByUserIdAndProvider(USER_ID, AuthProvider.GOOGLE))
                .thenReturn(Optional.empty());
        when(socialLinkPersistencePort.save(any(SocialLink.class), eq(USER_ID))).thenReturn(saved);

        SocialLink result = authService.linkSocial(new LinkSocialCommand(USER_ID, "google", "token"));

        assertThat(result).isSameAs(saved);
    }

    @Test
    void linkSocialRejectsWhenAlreadyLinked() {
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(socialTokenVerifier.verifyAndExtract(AuthProvider.GOOGLE, "token"))
                .thenReturn(new SocialUserProfile("puid", "s@example.com", "Social User"));
        when(socialLinkPersistencePort.existsByProviderAndProviderUserId(AuthProvider.GOOGLE, "puid"))
                .thenReturn(true);

        assertErrorCode(IdentityErrorCode.SOCIAL_LINK_EXISTS,
                () -> authService.linkSocial(new LinkSocialCommand(USER_ID, "google", "token")));
    }

    @Test
    void unlinkSocialRemovesLink() {
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(socialLinkPersistencePort.findByUserIdAndProvider(USER_ID, AuthProvider.GOOGLE))
                .thenReturn(Optional.of(SocialLink.createNew("sl-1", USER_ID, AuthProvider.GOOGLE, "puid")));

        authService.unlinkSocial(new UnlinkSocialCommand(USER_ID, "google"));

        verify(socialLinkPersistencePort).deleteByUserIdAndProvider(USER_ID, AuthProvider.GOOGLE);
    }

    @Test
    void unlinkSocialThrowsWhenLinkMissing() {
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(socialLinkPersistencePort.findByUserIdAndProvider(USER_ID, AuthProvider.GOOGLE))
                .thenReturn(Optional.empty());

        assertErrorCode(IdentityErrorCode.SOCIAL_LINK_NOT_FOUND,
                () -> authService.unlinkSocial(new UnlinkSocialCommand(USER_ID, "google")));
    }

    @Test
    void unlinkSocialRejectsWhenItIsTheOnlyLoginMethod() {
        IdentityUser noCredentials = IdentityUser.createNew(USER_ID, null, null, "user");
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(noCredentials));
        when(socialLinkPersistencePort.findByUserIdAndProvider(USER_ID, AuthProvider.GOOGLE))
                .thenReturn(Optional.of(SocialLink.createNew("sl-1", USER_ID, AuthProvider.GOOGLE, "puid")));

        assertErrorCode(IdentityErrorCode.SOCIAL_LINK_NOT_FOUND,
                () -> authService.unlinkSocial(new UnlinkSocialCommand(USER_ID, "google")));

        verify(socialLinkPersistencePort, never()).deleteByUserIdAndProvider(anyString(), any(AuthProvider.class));
    }

    @Test
    void revokeSessionThrowsWhenSessionMissing() {
        when(userPersistencePort.existsById(USER_ID)).thenReturn(true);
        when(authSessionPersistencePort.findById(SESSION_ID)).thenReturn(Optional.empty());

        assertErrorCode(IdentityErrorCode.SESSION_NOT_FOUND,
                () -> authService.revokeSession(new RevokeSessionCommand(USER_ID, SESSION_ID)));
    }

    @Test
    void revokeSessionThrowsWhenSessionBelongsToAnotherUser() {
        AuthSession other = AuthSession.createNew(SESSION_ID, "other-user", "ip", "ua",
                Instant.now().plus(Duration.ofDays(7)));
        when(userPersistencePort.existsById(USER_ID)).thenReturn(true);
        when(authSessionPersistencePort.findById(SESSION_ID)).thenReturn(Optional.of(other));

        assertErrorCode(IdentityErrorCode.SESSION_FORBIDDEN,
                () -> authService.revokeSession(new RevokeSessionCommand(USER_ID, SESSION_ID)));
    }

    @Test
    void revokeSessionIsNoOpWhenSessionAlreadyRevoked() {
        AuthSession session = activeSession();
        session.revoke();
        when(userPersistencePort.existsById(USER_ID)).thenReturn(true);
        when(authSessionPersistencePort.findById(SESSION_ID)).thenReturn(Optional.of(session));

        authService.revokeSession(new RevokeSessionCommand(USER_ID, SESSION_ID));

        verify(authSessionPersistencePort, never()).save(any(AuthSession.class));
        verify(refreshTokenStore, never()).revokeBySessionId(anyString());
    }

    @Test
    void refreshTokenIssuesNewTokensFromRequestToken() {
        RefreshAccessTokenResult expected = new RefreshAccessTokenResult(USER_ID, SESSION_ID, "access-token",
                "refresh", Instant.now(), Instant.now().plus(Duration.ofDays(7)));
        when(refreshTokenStore.consumeSessionId("rt")).thenReturn(Optional.of(SESSION_ID));
        when(authSessionPersistencePort.findById(SESSION_ID)).thenReturn(Optional.of(activeSession()));
        when(authPolicy.getSessionExpiresDays()).thenReturn(7L);
        stubTokenIssuance();
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(authResultMapper.toRefreshResult(any(AuthSession.class), eq("access-token"), anyString(),
                any(Instant.class))).thenReturn(expected);

        RefreshAccessTokenResult result = authService.refreshToken(
                new RefreshTokenCommand("rt", null, "ip", "ua"));

        assertThat(result).isSameAs(expected);
        verify(refreshTokenStore).store(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void refreshTokenFallsBackToCookieToken() {
        RefreshAccessTokenResult expected = new RefreshAccessTokenResult(USER_ID, SESSION_ID, "access-token",
                "refresh", Instant.now(), Instant.now().plus(Duration.ofDays(7)));
        when(refreshTokenStore.consumeSessionId("ct")).thenReturn(Optional.of(SESSION_ID));
        when(authSessionPersistencePort.findById(SESSION_ID)).thenReturn(Optional.of(activeSession()));
        when(authPolicy.getSessionExpiresDays()).thenReturn(7L);
        stubTokenIssuance();
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(authResultMapper.toRefreshResult(any(AuthSession.class), eq("access-token"), anyString(),
                any(Instant.class))).thenReturn(expected);

        RefreshAccessTokenResult result = authService.refreshToken(
                new RefreshTokenCommand("", "ct", "ip", "ua"));

        assertThat(result).isSameAs(expected);
    }

    @Test
    void refreshTokenRejectsWhenNoTokenProvided() {
        assertErrorCode(IdentityErrorCode.VERIFICATION_TOKEN_INVALID,
                () -> authService.refreshToken(new RefreshTokenCommand(null, "", "ip", "ua")));
    }

    @Test
    void refreshTokenRejectsWhenTokenAlreadyConsumed() {
        when(refreshTokenStore.consumeSessionId("rt")).thenReturn(Optional.empty());

        assertErrorCode(IdentityErrorCode.VERIFICATION_TOKEN_INVALID,
                () -> authService.refreshToken(new RefreshTokenCommand("rt", null, "ip", "ua")));
    }

    @Test
    void refreshTokenThrowsWhenSessionMissing() {
        when(refreshTokenStore.consumeSessionId("rt")).thenReturn(Optional.of(SESSION_ID));
        when(authSessionPersistencePort.findById(SESSION_ID)).thenReturn(Optional.empty());

        assertErrorCode(IdentityErrorCode.SESSION_NOT_FOUND,
                () -> authService.refreshToken(new RefreshTokenCommand("rt", null, "ip", "ua")));
    }

    @Test
    void refreshTokenRejectsExpiredSessionAndRevokesFamily() {
        when(refreshTokenStore.consumeSessionId("rt")).thenReturn(Optional.of(SESSION_ID));
        when(authSessionPersistencePort.findById(SESSION_ID)).thenReturn(Optional.of(expiredSession()));

        assertErrorCode(IdentityErrorCode.VERIFICATION_TOKEN_INVALID,
                () -> authService.refreshToken(new RefreshTokenCommand("rt", null, "ip", "ua")));

        verify(refreshTokenStore).revokeBySessionId(SESSION_ID);
    }

    @Test
    void refreshTokenRejectsRevokedSession() {
        AuthSession revoked = activeSession();
        revoked.revoke();
        when(refreshTokenStore.consumeSessionId("rt")).thenReturn(Optional.of(SESSION_ID));
        when(authSessionPersistencePort.findById(SESSION_ID)).thenReturn(Optional.of(revoked));

        assertErrorCode(IdentityErrorCode.VERIFICATION_TOKEN_INVALID,
                () -> authService.refreshToken(new RefreshTokenCommand("rt", null, "ip", "ua")));

        verify(refreshTokenStore).revokeBySessionId(SESSION_ID);
    }

}
