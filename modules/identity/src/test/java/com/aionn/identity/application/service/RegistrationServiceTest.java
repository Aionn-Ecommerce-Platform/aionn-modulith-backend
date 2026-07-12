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
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

        private static final Instant FIXED_NOW = Instant.parse("2026-07-12T10:00:00Z");

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
                                identityMetrics,
                                Clock.fixed(FIXED_NOW, java.time.ZoneOffset.UTC));
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
                assertThat(savedSession.getPhoneNumber()).isEqualTo(PhoneNumber.of("0912345678").toE164("VN"));
                assertThat(savedSession.getRegId()).isNotNull();
                assertThat(savedSession.getOtpCode()).isNotNull();
                assertThat(savedSession.isVerified()).isFalse();
                verify(notificationPort).sendRegistrationOtp(savedSession.getPhoneNumber(),
                                savedSession.getOtpCode());
                verify(registrationRateLimiter).check("IP", "203.0.113.10", 5, 60);
                verify(registrationRateLimiter).check("PHONE", "+84912345678", 3, 300);
                assertThat(result.regId()).isEqualTo(savedSession.getRegId());
                assertThat(result.otpCode()).isEqualTo(savedSession.getOtpCode());
        }

        @Test
        void initiateRejectsInvalidCaptchaBeforeTouchingPersistence() {
                when(captchaTokenValidator.isValid("captcha-bad")).thenReturn(false);

                IdentityException ex = assertThrows(IdentityException.class,
                                () -> registrationService.initiate(
                                                new InitiateRegistrationCommand("0912345678", "captcha-bad",
                                                                "203.0.113.10")));

                assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.CAPTCHA_INVALID.getCode());
                verifyNoInteractions(userPersistencePort, registrationRateLimiter, registrationSessionStore,
                                notificationPort);
        }

        @Test
        void completeCreatesUserSessionStoresRefreshTokenAndUnlocks() {
                when(registrationPolicy.getSessionExpiresDays()).thenReturn(7L);
                when(registrationPolicy.getLockTimeoutSeconds()).thenReturn(30);
                when(passwordHasher.hash("Password1!")).thenReturn("hashed-password");
                when(registrationLockManager.tryLock("+84912345678", 30)).thenReturn(Optional.of("lock-1"));
                when(userPersistencePort.existsByPhone("+84912345678")).thenReturn(false);
                when(userPersistencePort.existsByUsername("alice")).thenReturn(false);
                when(userPersistencePort.save(any(IdentityUser.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));
                when(authSessionPersistencePort.save(any(AuthSession.class))).thenAnswer(invocation -> {
                        AuthSession session = invocation.getArgument(0);
                        Instant now = FIXED_NOW;
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
                when(accessTokenIssuer.issueAccessToken(anyString(), eq("session-1"), any(Instant.class),
                                eq(Set.of("BUYER"))))
                                .thenReturn("access-token");
                Instant accessTokenExpiresAt = FIXED_NOW.plus(Duration.ofMinutes(15));
                when(accessTokenIssuer.extractExpiry("access-token"))
                                .thenReturn(Optional.of(accessTokenExpiresAt));
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
                                FIXED_NOW.minusSeconds(30),
                                FIXED_NOW.plusSeconds(600),
                                true,
                                "verify-token",
                                FIXED_NOW.minusSeconds(60));
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
                assertThat(savedUser.getPhone()).isEqualTo("+84912345678");
                assertThat(savedUser.getUsername()).isEqualTo("alice");
                assertThat(savedUser.getPasswordHash()).isEqualTo("hashed-password");
                assertThat(savedUser.getDisplayName()).isEqualTo("alice");
                assertThat(savedUser.getPhoneVerifiedAt()).isNotNull();
                assertThat(savedUser.getRoles()).contains(UserRole.BUYER);

                verify(accessTokenIssuer).issueAccessToken(
                                eq(savedUser.getUserId()),
                                eq("session-1"),
                                any(Instant.class),
                                eq(Set.of("BUYER")));
                verify(refreshTokenStore).store(
                                anyString(),
                                eq("session-1"),
                                argThat(ttl -> ttl.compareTo(Duration.ofDays(6)) > 0));
                verify(registrationSessionStore).deleteByRegId("reg-1");
                verify(registrationLockManager).unlockAfterCompletion("+84912345678", "lock-1");

                assertThat(result.userId()).isEqualTo(savedUser.getUserId());
                assertThat(result.sessionId()).isEqualTo("session-1");
                assertThat(result.accessToken()).isEqualTo("access-token");
                assertThat(result.refreshToken()).isNotBlank();
                assertThat(result.expiresAt()).isEqualTo(accessTokenExpiresAt);
        }

        @Test
        void completeUnlocksRegistrationWhenVerificationFails() {
                RegistrationVerificationSession session = new RegistrationVerificationSession(
                                "reg-1",
                                "+84912345678",
                                null,
                                0,
                                5,
                                FIXED_NOW.minusSeconds(30),
                                FIXED_NOW.plusSeconds(600),
                                true,
                                "expected-token",
                                FIXED_NOW.minusSeconds(60));
                when(registrationSessionStore.findByRegId("reg-1")).thenReturn(Optional.of(session));
                when(registrationPolicy.getLockTimeoutSeconds()).thenReturn(30);
                when(registrationLockManager.tryLock("+84912345678", 30)).thenReturn(Optional.of("lock-1"));

                IdentityException ex = assertThrows(IdentityException.class,
                                () -> registrationService.complete(new CompleteRegistrationCommand(
                                                "reg-1",
                                                "Password1!",
                                                "alice",
                                                "wrong-token",
                                                "198.51.100.20",
                                                "JUnit/1.0")));

                assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.VERIFICATION_TOKEN_INVALID.getCode());
                verify(registrationLockManager).unlockAfterCompletion("+84912345678", "lock-1");
                verify(userPersistencePort, never()).save(any());
                verify(authSessionPersistencePort, never()).save(any());
                verify(refreshTokenStore, never()).store(anyString(), anyString(), any(Duration.class));
                verify(registrationSessionStore, never()).deleteByRegId(anyString());
        }

        @Test
        void initiateRejectsWhenPhoneAlreadyExists() {
                when(captchaTokenValidator.isValid("captcha-ok")).thenReturn(true);
                when(registrationPolicy.getDefaultCountryCallingCode()).thenReturn("VN");
                when(userPersistencePort.existsByPhone("+84912345678")).thenReturn(true);

                IdentityException ex = assertThrows(IdentityException.class,
                                () -> registrationService.initiate(
                                                new InitiateRegistrationCommand("0912345678", "captcha-ok",
                                                                "203.0.113.10")));

                assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.PHONE_ALREADY_EXISTS.getCode());
                verify(registrationSessionStore, never()).save(any());
        }

        @Test
        void initiateRejectsInvalidPhoneNumber() {
                when(captchaTokenValidator.isValid("captcha-ok")).thenReturn(true);

                IdentityException ex = assertThrows(IdentityException.class,
                                () -> registrationService.initiate(
                                                new InitiateRegistrationCommand("not-a-phone", "captcha-ok",
                                                                "203.0.113.10")));

                assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.PHONE_INVALID.getCode());
        }

        @Test
        void initiateRejectsWhenRateLimitExceeded() {
                when(captchaTokenValidator.isValid("captcha-ok")).thenReturn(true);
                when(registrationPolicy.getDefaultCountryCallingCode()).thenReturn("VN");
                when(userPersistencePort.existsByPhone("+84912345678")).thenReturn(false);
                when(registrationPolicy.getIpRateLimitMaxAttempts()).thenReturn(5);
                when(registrationPolicy.getIpRateLimitWindowSeconds()).thenReturn(60);
                when(registrationPolicy.getPhoneRateLimitMaxAttempts()).thenReturn(3);
                when(registrationPolicy.getPhoneRateLimitWindowSeconds()).thenReturn(300);
                when(registrationRateLimiter.check("IP", "203.0.113.10", 5, 60)).thenReturn(false);

                IdentityException ex = assertThrows(IdentityException.class,
                                () -> registrationService.initiate(
                                                new InitiateRegistrationCommand("0912345678", "captcha-ok",
                                                                "203.0.113.10")));

                assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.RATE_LIMIT_EXCEEDED.getCode());
                verify(registrationSessionStore, never()).save(any());
        }

        @Test
        void verifyOtpReturnsExistingTokenWhenAlreadyVerified() {
                RegistrationVerificationSession session = new RegistrationVerificationSession(
                                "reg-1", "+84912345678", null, 0, 5,
                                FIXED_NOW.minusSeconds(30),
                                FIXED_NOW.plusSeconds(600),
                                true, "verify-token", FIXED_NOW.minusSeconds(60));
                when(registrationSessionStore.findByRegId("reg-1")).thenReturn(Optional.of(session));
                when(registrationResultMapper.toVerifyOtpResult("reg-1", "verify-token"))
                                .thenReturn(new com.aionn.identity.application.dto.registration.result.VerifyRegistrationOtpResult(
                                                "reg-1", "verify-token"));

                var result = registrationService.verifyOtp(
                                new com.aionn.identity.application.dto.registration.command.VerifyRegistrationOtpCommand(
                                                "reg-1", "123456"));

                assertThat(result.verificationToken()).isEqualTo("verify-token");
                verify(registrationSessionStore, never()).save(any());
        }

        @Test
        void verifyOtpThrowsWhenSessionNotFound() {
                when(registrationSessionStore.findByRegId("missing")).thenReturn(Optional.empty());

                IdentityException ex = assertThrows(IdentityException.class,
                                () -> registrationService.verifyOtp(
                                                new com.aionn.identity.application.dto.registration.command.VerifyRegistrationOtpCommand(
                                                                "missing", "123456")));

                assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.REGISTRATION_SESSION_NOT_FOUND.getCode());
        }

        @Test
        void verifyOtpMarksSessionVerifiedOnCorrectCode() {
                RegistrationVerificationSession session = new RegistrationVerificationSession(
                                "reg-1", "+84912345678", "123456", 0, 5,
                                FIXED_NOW.minusSeconds(30),
                                FIXED_NOW.plusSeconds(600),
                                false, null, null);
                when(registrationSessionStore.findByRegId("reg-1")).thenReturn(Optional.of(session));
                when(registrationResultMapper.toVerifyOtpResult(eq("reg-1"), anyString()))
                                .thenAnswer(inv -> new com.aionn.identity.application.dto.registration.result.VerifyRegistrationOtpResult(
                                                inv.getArgument(0), inv.getArgument(1)));

                var result = registrationService.verifyOtp(
                                new com.aionn.identity.application.dto.registration.command.VerifyRegistrationOtpCommand(
                                                "reg-1", "123456"));

                assertThat(session.isVerified()).isTrue();
                assertThat(result.verificationToken()).isNotNull();
                verify(registrationSessionStore).save(session);
        }

        @Test
        void resendOtpThrowsWhenSessionNotFound() {
                when(registrationSessionStore.findByRegId("missing")).thenReturn(Optional.empty());

                assertThrows(IdentityException.class,
                                () -> registrationService.resendOtp(
                                                new com.aionn.identity.application.dto.registration.command.ResendRegistrationOtpCommand(
                                                                "missing", "203.0.113.10")));
        }

        @Test
        void resendOtpGeneratesNewCodeAndNotifies() {
                when(registrationPolicy.getResendCooldownSeconds()).thenReturn(60);
                when(registrationPolicy.getOtpExpirySeconds()).thenReturn(300);
                when(registrationPolicy.isExposeOtpInResponse()).thenReturn(false);
                RegistrationVerificationSession session = new RegistrationVerificationSession(
                                "reg-1", "+84912345678", "123456", 0, 5,
                                FIXED_NOW.minusSeconds(60),
                                FIXED_NOW.plusSeconds(600),
                                false, null, null);
                when(registrationSessionStore.findByRegId("reg-1")).thenReturn(Optional.of(session));
                when(registrationResultMapper.toResendOtpResult(any(RegistrationVerificationSession.class), any()))
                                .thenReturn(new com.aionn.identity.application.dto.registration.result.ResendRegistrationOtpResult(
                                                "reg-1", session.getResendAvailableAt(), session.getExpiredAt(), null));

                registrationService.resendOtp(
                                new com.aionn.identity.application.dto.registration.command.ResendRegistrationOtpCommand(
                                                "reg-1", "203.0.113.10"));

                verify(registrationSessionStore).save(session);
                verify(notificationPort).sendRegistrationOtp(eq("+84912345678"), anyString());
        }

        @Test
        void completeThrowsWhenSessionNotFound() {
                when(registrationSessionStore.findByRegId("missing")).thenReturn(Optional.empty());

                assertThrows(IdentityException.class,
                                () -> registrationService.complete(new CompleteRegistrationCommand(
                                                "missing", "Password1!", "alice", "verify-token",
                                                "198.51.100.20", "JUnit/1.0")));
        }

        @Test
        void completeThrowsWhenLockCannotBeAcquired() {
                RegistrationVerificationSession session = new RegistrationVerificationSession(
                                "reg-1", "+84912345678", null, 0, 5,
                                FIXED_NOW.minusSeconds(30),
                                FIXED_NOW.plusSeconds(600),
                                true, "verify-token", FIXED_NOW.minusSeconds(60));
                when(registrationSessionStore.findByRegId("reg-1")).thenReturn(Optional.of(session));
                when(registrationPolicy.getLockTimeoutSeconds()).thenReturn(30);
                when(registrationLockManager.tryLock("+84912345678", 30)).thenReturn(Optional.empty());

                IdentityException ex = assertThrows(IdentityException.class,
                                () -> registrationService.complete(new CompleteRegistrationCommand(
                                                "reg-1", "Password1!", "alice", "verify-token",
                                                "198.51.100.20", "JUnit/1.0")));

                assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.REGISTRATION_IN_PROGRESS.getCode());
                verify(userPersistencePort, never()).save(any());
        }

        @Test
        void completeThrowsWhenSessionExpired() {
                RegistrationVerificationSession session = new RegistrationVerificationSession(
                                "reg-1", "+84912345678", null, 0, 5,
                                FIXED_NOW.minusSeconds(300),
                                FIXED_NOW.minusSeconds(60),
                                true, "verify-token", FIXED_NOW.minusSeconds(240));
                when(registrationSessionStore.findByRegId("reg-1")).thenReturn(Optional.of(session));
                when(registrationPolicy.getLockTimeoutSeconds()).thenReturn(30);
                when(registrationLockManager.tryLock("+84912345678", 30)).thenReturn(Optional.of("lock-1"));

                IdentityException ex = assertThrows(IdentityException.class,
                                () -> registrationService.complete(new CompleteRegistrationCommand(
                                                "reg-1", "Password1!", "alice", "verify-token",
                                                "198.51.100.20", "JUnit/1.0")));

                assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.REGISTRATION_SESSION_EXPIRED.getCode());
                verify(registrationLockManager).unlockAfterCompletion("+84912345678", "lock-1");
        }

        @Test
        void completeThrowsWhenUsernameAlreadyExists() {
                RegistrationVerificationSession session = new RegistrationVerificationSession(
                                "reg-1", "+84912345678", null, 0, 5,
                                FIXED_NOW.minusSeconds(30),
                                FIXED_NOW.plusSeconds(600),
                                true, "verify-token", FIXED_NOW.minusSeconds(60));
                when(registrationSessionStore.findByRegId("reg-1")).thenReturn(Optional.of(session));
                when(registrationPolicy.getLockTimeoutSeconds()).thenReturn(30);
                when(registrationLockManager.tryLock("+84912345678", 30)).thenReturn(Optional.of("lock-1"));
                when(userPersistencePort.existsByPhone("+84912345678")).thenReturn(false);
                when(userPersistencePort.existsByUsername("alice")).thenReturn(true);

                IdentityException ex = assertThrows(IdentityException.class,
                                () -> registrationService.complete(new CompleteRegistrationCommand(
                                                "reg-1", "Password1!", "alice", "verify-token",
                                                "198.51.100.20", "JUnit/1.0")));

                assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.USERNAME_ALREADY_EXISTS.getCode());
                verify(userPersistencePort, never()).save(any());
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
