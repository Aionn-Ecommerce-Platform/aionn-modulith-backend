package com.aionn.identity.application.service;

import com.aionn.identity.application.dto.registration.command.CompleteRegistrationCommand;
import com.aionn.identity.application.dto.registration.command.InitiateRegistrationCommand;
import com.aionn.identity.application.dto.registration.result.CompleteRegistrationResult;
import com.aionn.identity.application.dto.registration.result.InitiateRegistrationResult;
import com.aionn.identity.application.mapper.RegistrationResultMapper;
import com.aionn.identity.application.policy.RegistrationPolicy;
import com.aionn.identity.application.port.out.auth.AccessTokenIssuerPort;
import com.aionn.identity.application.port.out.auth.AuthSessionPersistencePort;
import com.aionn.identity.application.port.out.auth.RefreshTokenStorePort;
import com.aionn.identity.application.port.out.observability.IdentityMetricsPort;
import com.aionn.sharedkernel.integration.port.notification.IdentityNotificationPort;
import com.aionn.identity.application.port.out.registration.CaptchaTokenValidatorPort;
import com.aionn.identity.application.port.out.registration.RegistrationLockManagerPort;
import com.aionn.identity.application.port.out.registration.RegistrationRateLimiterPort;
import com.aionn.identity.application.port.out.registration.RegistrationSessionStorePort;
import com.aionn.identity.application.port.out.security.PasswordHasherPort;
import com.aionn.identity.application.port.out.user.UserPersistencePort;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.model.AuthSession;
import com.aionn.identity.domain.model.IdentityUser;
import com.aionn.identity.domain.model.RegistrationVerificationSession;
import com.aionn.identity.domain.valueobject.AuthSessionStatus;
import com.aionn.identity.domain.valueobject.UserRole;
import com.aionn.sharedkernel.domain.vo.PhoneNumber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

        @Mock
        private UserPersistencePort userPersistencePort;
        @Mock
        private AuthSessionPersistencePort authSessionPersistencePort;
        @Mock
        private IdentityNotificationPort notificationPort;
        @Mock
        private CaptchaTokenValidatorPort captchaTokenValidator;
        @Mock
        private RegistrationRateLimiterPort registrationRateLimiter;
        @Mock
        private RegistrationSessionStorePort registrationSessionStore;
        @Mock
        private AccessTokenIssuerPort accessTokenIssuer;
        @Mock
        private RefreshTokenStorePort refreshTokenStore;
        @Mock
        private PasswordHasherPort passwordHasher;
        @Mock
        private RegistrationResultMapper registrationResultMapper;
        @Mock
        private RegistrationPolicy registrationPolicy;
        @Mock
        private RegistrationLockManagerPort registrationLockManager;
        @Mock
        private IdentityMetricsPort identityMetrics;

        private RegistrationService registrationService;

        @BeforeEach
        void setUp() {
                registrationService = new RegistrationService(
                                userPersistencePort,
                                authSessionPersistencePort,
                                notificationPort,
                                captchaTokenValidator,
                                registrationRateLimiter,
                                registrationSessionStore,
                                accessTokenIssuer,
                                refreshTokenStore,
                                passwordHasher,
                                registrationResultMapper,
                                registrationPolicy,
                                registrationLockManager,
                                identityMetrics);
        }

        @Test
        void initiateStoresNormalizedSessionAndSendsOtp() {
                stubCommonRegistrationPolicy();
                when(captchaTokenValidator.isValid("captcha-ok")).thenReturn(true);
                when(userPersistencePort.existsByPhone("+84912345678")).thenReturn(false);
                when(registrationRateLimiter.check("IP", "203.0.113.10", 5, 60)).thenReturn(true);
                when(registrationRateLimiter.check("PHONE", "+84912345678", 3, 300)).thenReturn(true);
                when(registrationResultMapper.toInitiateResult(any(RegistrationVerificationSession.class), anyString()))
                                .thenAnswer(invocation -> {
                                        RegistrationVerificationSession session = invocation.getArgument(0);
                                        String otpCode = invocation.getArgument(1);
                                        return new InitiateRegistrationResult(
                                                        session.getRegId(),
                                                        session.getResendAvailableAt(),
                                                        session.getExpiredAt(),
                                                        otpCode);
                                });

                InitiateRegistrationResult result = registrationService.initiate(
                                new InitiateRegistrationCommand("0912345678", "captcha-ok", "203.0.113.10"));

                ArgumentCaptor<RegistrationVerificationSession> sessionCaptor = ArgumentCaptor
                                .forClass(RegistrationVerificationSession.class);
                verify(registrationSessionStore).save(sessionCaptor.capture());

                RegistrationVerificationSession savedSession = sessionCaptor.getValue();
                assertEquals(PhoneNumber.of("0912345678").toE164("VN"), savedSession.getPhoneNumber());
                assertNotNull(savedSession.getRegId());
                assertNotNull(savedSession.getOtpCode());
                assertFalse(savedSession.isVerified());
                verify(notificationPort).sendRegistrationOtp(savedSession.getPhoneNumber(),
                                savedSession.getOtpCode());
                verify(registrationRateLimiter).check("IP", "203.0.113.10", 5, 60);
                verify(registrationRateLimiter).check("PHONE", "+84912345678", 3, 300);
                assertEquals(savedSession.getRegId(), result.regId());
                assertEquals(savedSession.getOtpCode(), result.otpCode());
        }

        @Test
        void initiateRejectsInvalidCaptchaBeforeTouchingPersistence() {
                when(captchaTokenValidator.isValid("captcha-bad")).thenReturn(false);

                IdentityException ex = assertThrows(IdentityException.class,
                                () -> registrationService.initiate(
                                                new InitiateRegistrationCommand("0912345678", "captcha-bad",
                                                                "203.0.113.10")));

                assertEquals(IdentityErrorCode.CAPTCHA_INVALID.getCode(), ex.getErrorCode());
                verifyNoInteractions(userPersistencePort, registrationRateLimiter, registrationSessionStore,
                                notificationPort);
        }

        @Test
        void completeCreatesUserSessionStoresRefreshTokenAndUnlocks() {
                when(registrationPolicy.getSessionExpiresDays()).thenReturn(7L);
                when(registrationPolicy.getLockTimeoutSeconds()).thenReturn(30);
                when(passwordHasher.hash("Password1!")).thenReturn("hashed-password");
                when(registrationLockManager.tryLock("+84912345678", 30)).thenReturn("lock-1");
                when(userPersistencePort.existsByPhone("+84912345678")).thenReturn(false);
                when(userPersistencePort.existsByUsername("alice")).thenReturn(false);
                when(userPersistencePort.save(any(IdentityUser.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));
                when(authSessionPersistencePort.save(any(AuthSession.class))).thenAnswer(invocation -> {
                        AuthSession session = invocation.getArgument(0);
                        LocalDateTime now = LocalDateTime.now();
                        return new AuthSession(
                                        "session-1",
                                        session.getUserId(),
                                        session.getIpAddress(),
                                        session.getUserAgent(),
                                        AuthSessionStatus.ACTIVE,
                                        now,
                                        now,
                                        session.getExpiresAt());
                });
                when(accessTokenIssuer.issueAccessToken(anyString(), eq("session-1"), any(LocalDateTime.class),
                                eq(Set.of("BUYER"))))
                                .thenReturn("access-token");
                LocalDateTime accessTokenExpiresAt = LocalDateTime.now().plusMinutes(15);
                when(accessTokenIssuer.extractExpiry("access-token")).thenReturn(accessTokenExpiresAt);
                when(registrationResultMapper.toCompleteResult(any(AuthSession.class), eq("access-token"), anyString(),
                                eq(accessTokenExpiresAt)))
                                .thenAnswer(invocation -> {
                                        AuthSession session = invocation.getArgument(0);
                                        String refreshToken = invocation.getArgument(2);
                                        return new CompleteRegistrationResult(
                                                        session.getUserId(),
                                                        session.getSessionId(),
                                                        refreshToken,
                                                        invocation.getArgument(1),
                                                        invocation.getArgument(3),
                                                        session.getExpiresAt());
                                });

                RegistrationVerificationSession session = new RegistrationVerificationSession(
                                "reg-1",
                                "+84912345678",
                                null,
                                0,
                                5,
                                LocalDateTime.now().minusSeconds(30),
                                LocalDateTime.now().plusMinutes(10),
                                true,
                                "verify-token",
                                LocalDateTime.now().minusMinutes(1));
                when(registrationSessionStore.findByRegId("reg-1")).thenReturn(Optional.of(session));

                CompleteRegistrationResult result = registrationService.complete(new CompleteRegistrationCommand(
                                "reg-1",
                                "Password1!",
                                "alice",
                                "verify-token",
                                "198.51.100.20",
                                "JUnit/1.0"));

                ArgumentCaptor<IdentityUser> userCaptor = ArgumentCaptor.forClass(IdentityUser.class);
                verify(userPersistencePort).save(userCaptor.capture());
                IdentityUser savedUser = userCaptor.getValue();
                assertEquals("+84912345678", savedUser.getPhone());
                assertEquals("alice", savedUser.getUsername());
                assertEquals("hashed-password", savedUser.getPasswordHash());
                assertEquals("alice", savedUser.getDisplayName());
                assertNotNull(savedUser.getPhoneVerifiedAt());
                assertTrue(savedUser.getRoles().contains(UserRole.BUYER));

                verify(accessTokenIssuer).issueAccessToken(
                                eq(savedUser.getUserId()),
                                eq("session-1"),
                                any(LocalDateTime.class),
                                eq(Set.of("BUYER")));
                verify(refreshTokenStore).store(
                                anyString(),
                                eq("session-1"),
                                argThat(ttl -> ttl.compareTo(Duration.ofDays(6)) > 0));
                verify(registrationSessionStore).deleteByRegId("reg-1");
                verify(registrationLockManager).unlockAfterCompletion("+84912345678", "lock-1");

                assertEquals(savedUser.getUserId(), result.userId());
                assertEquals("session-1", result.sessionId());
                assertEquals("access-token", result.accessToken());
                assertNotNull(result.refreshToken());
                assertFalse(result.refreshToken().isBlank());
                assertEquals(accessTokenExpiresAt, result.expiresAt());
        }

        @Test
        void completeUnlocksRegistrationWhenVerificationFails() {
                RegistrationVerificationSession session = new RegistrationVerificationSession(
                                "reg-1",
                                "+84912345678",
                                null,
                                0,
                                5,
                                LocalDateTime.now().minusSeconds(30),
                                LocalDateTime.now().plusMinutes(10),
                                true,
                                "expected-token",
                                LocalDateTime.now().minusMinutes(1));
                when(registrationSessionStore.findByRegId("reg-1")).thenReturn(Optional.of(session));
                when(registrationPolicy.getLockTimeoutSeconds()).thenReturn(30);
                when(registrationLockManager.tryLock("+84912345678", 30)).thenReturn("lock-1");

                IdentityException ex = assertThrows(IdentityException.class,
                                () -> registrationService.complete(new CompleteRegistrationCommand(
                                                "reg-1",
                                                "Password1!",
                                                "alice",
                                                "wrong-token",
                                                "198.51.100.20",
                                                "JUnit/1.0")));

                assertEquals(IdentityErrorCode.VERIFICATION_TOKEN_INVALID.getCode(), ex.getErrorCode());
                verify(registrationLockManager).unlockAfterCompletion("+84912345678", "lock-1");
                verify(userPersistencePort, never()).save(any());
                verify(authSessionPersistencePort, never()).save(any());
                verify(refreshTokenStore, never()).store(anyString(), anyString(), any(Duration.class));
                verify(registrationSessionStore, never()).deleteByRegId(anyString());
        }

        private void stubCommonRegistrationPolicy() {
                when(registrationPolicy.getMaxVerifyAttempts()).thenReturn(5);
                when(registrationPolicy.getResendCooldownSeconds()).thenReturn(60);
                when(registrationPolicy.getOtpExpirySeconds()).thenReturn(300);
                when(registrationPolicy.getDefaultCountryCallingCode()).thenReturn("VN");
                when(registrationPolicy.isExposeOtpInResponse()).thenReturn(true);
                when(registrationPolicy.getIpRateLimitMaxAttempts()).thenReturn(5);
                when(registrationPolicy.getIpRateLimitWindowSeconds()).thenReturn(60);
                when(registrationPolicy.getPhoneRateLimitMaxAttempts()).thenReturn(3);
                when(registrationPolicy.getPhoneRateLimitWindowSeconds()).thenReturn(300);
        }
}
