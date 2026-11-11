package com.aionn.identity.application.service;

import com.aionn.identity.domain.valueobject.UserStatus;
import com.aionn.sharedkernel.testing.fixture.TestClocks;
import java.time.ZoneOffset;

import com.aionn.identity.application.dto.user.command.CancelAccountDeletionCommand;
import com.aionn.identity.application.dto.user.command.RequestAccountDeletionCommand;
import com.aionn.identity.application.dto.user.command.RequestDataExportCommand;
import com.aionn.identity.application.dto.user.view.DataExportRequestView;
import com.aionn.identity.application.dto.user.view.DeletionRequestView;
import com.aionn.identity.application.mapper.UserResultMapper;
import com.aionn.identity.application.policy.AccountManagementPolicy;
import com.aionn.identity.application.port.out.auth.AuthSessionPersistencePort;
import com.aionn.identity.application.port.out.auth.RefreshTokenStorePort;
import com.aionn.identity.application.port.out.integration.IdentityIntegrationEventPublisherPort;
import com.aionn.identity.application.port.out.user.AccountDeletionPort;
import com.aionn.identity.application.port.out.user.DataExportPort;
import com.aionn.identity.application.port.out.user.UserOtpChallengeStorePort;
import com.aionn.identity.application.port.out.user.UserOtpChallengeStorePort.UserOtpChallenge;
import com.aionn.identity.application.port.out.user.UserPersistencePort;
import com.aionn.identity.application.dto.user.view.UserProfileView;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.model.AuthSession;
import com.aionn.identity.domain.model.IdentityUser;
import com.aionn.identity.domain.valueobject.OtpChannel;
import com.aionn.identity.domain.valueobject.UserOtpPurpose;
import com.aionn.contracts.port.notification.IdentityNotificationPort;
import com.aionn.identity.application.port.out.security.MfaPersistencePort;
import com.aionn.identity.application.port.out.security.PasswordHasherPort;
import com.aionn.identity.application.port.out.security.TotpManagerPort;
import com.aionn.identity.application.port.out.security.TotpReplayGuardPort;
import com.aionn.identity.application.port.out.security.UserSecurityPort;
import com.aionn.contracts.port.catalog.MerchantQueryPort;
import com.aionn.contracts.port.ordering.OrderQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class AccountManagementServiceTest {

        private static final String USER_ID = "user-1";

        @Mock
        private UserPersistencePort userPersistencePort;
        @Mock
        private IdentityNotificationPort notificationPort;
        @Mock
        private IdentityIntegrationEventPublisherPort integrationEventPublisher;
        @Mock
        private UserOtpChallengeStorePort userOtpChallengeStore;
        @Mock
        private AccountDeletionPort accountDeletionPort;
        @Mock
        private DataExportPort dataExportPort;
        @Mock
        private AuthSessionPersistencePort authSessionPersistencePort;
        @Mock
        private RefreshTokenStorePort refreshTokenStore;
        @Mock
        private AccountManagementPolicy accountManagementPolicy;
        @Mock
        private UserResultMapper userResultMapper;
        @Mock
        private UserSecurityPort userSecurityPort;
        @Mock
        private PasswordHasherPort passwordHasher;
        @Mock
        private TotpManagerPort totpManager;
        @Mock
        private TotpReplayGuardPort totpReplayGuard;
        @Mock
        private MfaPersistencePort mfaPersistencePort;
        @Mock
        private OrderQueryPort orderQueryPort;
        @Mock
        private MerchantQueryPort merchantQueryPort;

        private static final Instant FIXED_NOW = Instant.parse("2026-07-12T10:00:00Z");

        private AccountManagementService service;

        @BeforeEach
        void setUp() {
                service = new AccountManagementService(
                                userPersistencePort, notificationPort, integrationEventPublisher,
                                userOtpChallengeStore, accountDeletionPort, dataExportPort,
                                authSessionPersistencePort, refreshTokenStore,
                                accountManagementPolicy, userResultMapper,
                                userSecurityPort, passwordHasher, totpManager,
                                totpReplayGuard,
                                mfaPersistencePort, orderQueryPort, merchantQueryPort,
                                Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
                org.mockito.Mockito.lenient()
                                .when(userOtpChallengeStore.tryAcquireAttempt(
                                                any(), any(), any(Integer.class), any()))
                                .thenReturn(true);
        }

        private static IdentityUser activeUser() {
                return IdentityUser.createNew(USER_ID, "u@example.com", null, "user", TestClocks.FIXED);
        }

        @Test
        void requestAccountDeletionPersistsAndReturnsView() {
                DeletionRequestView view = new DeletionRequestView(
                                "req-1", "PENDING", FIXED_NOW, FIXED_NOW.plus(Duration.ofDays(30)));
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
                when(userSecurityPort.findById(USER_ID)).thenReturn(Optional.of(
                                new UserSecurityPort.UserSecurityData(USER_ID, null, UserStatus.ACTIVE, false, null, null, 0)));
                when(orderQueryPort.hasOpenOrdersForUser(USER_ID)).thenReturn(false);
                when(merchantQueryPort.findMerchantIdByOwnerId(USER_ID)).thenReturn(Optional.empty());
                when(accountDeletionPort.findPendingByUserId(USER_ID)).thenReturn(Optional.empty());
                when(accountManagementPolicy.getDeletionGraceDays()).thenReturn(30);
                when(accountDeletionPort.save(eq(USER_ID), any(Instant.class))).thenReturn(view);

                DeletionRequestView result = service.requestAccountDeletion(
                                new RequestAccountDeletionCommand(USER_ID));

                assertThat(result).isSameAs(view);
                verify(accountDeletionPort).save(eq(USER_ID), any(Instant.class));
        }

        @Test
        void requestAccountDeletionRejectsWhenAlreadyPending() {
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
                when(userSecurityPort.findById(USER_ID)).thenReturn(Optional.of(
                                new UserSecurityPort.UserSecurityData(USER_ID, null, UserStatus.ACTIVE, false, null, null, 0)));
                when(orderQueryPort.hasOpenOrdersForUser(USER_ID)).thenReturn(false);
                when(merchantQueryPort.findMerchantIdByOwnerId(USER_ID)).thenReturn(Optional.empty());
                when(accountDeletionPort.findPendingByUserId(USER_ID))
                                .thenReturn(Optional.of(new DeletionRequestView(
                                                "req-old", "PENDING", FIXED_NOW,
                                                FIXED_NOW.plus(Duration.ofDays(10)))));

                assertThrows(IdentityException.class,
                                () -> service.requestAccountDeletion(new RequestAccountDeletionCommand(USER_ID)));

                verify(accountDeletionPort, never()).save(any(), any());
        }

        @Test
        void requestAccountDeletionRejectsWhenHasOpenOrders() {
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
                when(userSecurityPort.findById(USER_ID)).thenReturn(Optional.of(
                                new UserSecurityPort.UserSecurityData(USER_ID, null, UserStatus.ACTIVE, false, null, null, 0)));
                when(orderQueryPort.hasOpenOrdersForUser(USER_ID)).thenReturn(true);

                RequestAccountDeletionCommand command = new RequestAccountDeletionCommand(USER_ID);
                IdentityException ex = assertThrows(IdentityException.class,
                                () -> service.requestAccountDeletion(command));

                assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.ACCOUNT_HAS_OPEN_ORDERS.getCode());
                verify(accountDeletionPort, never()).save(any(), any());
        }

        @Test
        void cancelAccountDeletionDelegatesToPort() {
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
                when(accountDeletionPort.cancel(USER_ID)).thenReturn(true);

                service.cancelAccountDeletion(new CancelAccountDeletionCommand(USER_ID));

                verify(accountDeletionPort).cancel(USER_ID);
        }

        @Test
        void cancelAccountDeletionThrowsWhenNothingToCancel() {
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
                when(accountDeletionPort.cancel(USER_ID)).thenReturn(false);

                assertThrows(IdentityException.class,
                                () -> service.cancelAccountDeletion(new CancelAccountDeletionCommand(USER_ID)));
        }

        @Test
        void requestDataExportRejectsWhenAlreadyInProgress() {
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
                when(dataExportPort.save(USER_ID))
                                .thenThrow(new IdentityException(IdentityErrorCode.DATA_EXPORT_ALREADY_IN_PROGRESS));

                assertThrows(IdentityException.class,
                                () -> service.requestDataExport(new RequestDataExportCommand(USER_ID)));
        }

        @Test
        void requestDataExportPersistsAndReturnsView() {
                DataExportRequestView view = new DataExportRequestView(
                                "exp-1", "COMPLETED", FIXED_NOW);
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
                when(dataExportPort.save(USER_ID)).thenReturn(view);

                DataExportRequestView result = service.requestDataExport(
                                new RequestDataExportCommand(USER_ID));

                assertThat(result).isSameAs(view);
        }

        private static IdentityUser bannedUser() {
                IdentityUser user = IdentityUser.createNew(USER_ID, "u@example.com", null, "user", TestClocks.FIXED);
                user.ban();
                return user;
        }

        private static UserOtpChallenge challenge(
                        UserOtpPurpose purpose, OtpChannel channel, String target,
                        String otpCode, String pendingValue, Instant expiresAt, int attempts) {
                return new UserOtpChallenge(USER_ID, purpose, channel, target, otpCode, pendingValue, expiresAt,
                                attempts);
        }

        private static Instant future() {
                return FIXED_NOW.plus(Duration.ofDays(1));
        }

        private static Instant past() {
                return FIXED_NOW.minus(Duration.ofDays(1));
        }

        private static AuthSession activeSession() {
                return AuthSession.createNew("session-1", USER_ID, "ip", "ua",
                                FIXED_NOW.plus(Duration.ofDays(7)),
                                Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
        }

        private static UserProfileView profileView() {
                return new UserProfileView(USER_ID, "u@example.com", null, "user", null, null,
                                Set.of("BUYER"), "ACTIVE", null, null, FIXED_NOW);
        }

        private static void assertErrorCode(IdentityErrorCode expected, Executable executable) {
                IdentityException ex = assertThrows(IdentityException.class, executable);
                assertThat(ex.getErrorCode()).isEqualTo(expected.getCode());
        }

        @Test
        void sendVerifyPrimaryEmailOtpSendsAndStoresChallenge() {
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
                when(accountManagementPolicy.getOtpExpirySeconds()).thenReturn(300);

                service.sendVerifyPrimaryEmailOtp(USER_ID);

                verify(userOtpChallengeStore).save(any(UserOtpChallenge.class));
                verify(notificationPort).sendEmailOtp(eq("u@example.com"), anyString());
        }

        @Test
        void sendVerifyPrimaryEmailOtpThrowsWhenNoPrimaryEmail() {
                when(userPersistencePort.findById(USER_ID))
                                .thenReturn(Optional.of(IdentityUser.createNew(USER_ID, null, null, "user", TestClocks.FIXED)));

                assertErrorCode(IdentityErrorCode.EMAIL_REQUIRED,
                                () -> service.sendVerifyPrimaryEmailOtp(USER_ID));
        }

        @Test
        void getActiveUserThrowsWhenUserMissing() {
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.empty());

                assertErrorCode(IdentityErrorCode.USER_NOT_FOUND,
                                () -> service.sendVerifyPrimaryEmailOtp(USER_ID));
        }

        @Test
        void getActiveUserThrowsWhenUserInactive() {
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(bannedUser()));

                assertErrorCode(IdentityErrorCode.USER_INACTIVE,
                                () -> service.sendVerifyPrimaryEmailOtp(USER_ID));
        }

        @Test
        void confirmVerifyPrimaryEmailOtpVerifiesEmail() {
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
                when(userOtpChallengeStore.find(USER_ID, UserOtpPurpose.VERIFY_PRIMARY_EMAIL)).thenReturn(
                                Optional.of(challenge(UserOtpPurpose.VERIFY_PRIMARY_EMAIL, OtpChannel.EMAIL,
                                                "u@example.com", "123456", null, future(), 0)));

                service.confirmVerifyPrimaryEmailOtp(USER_ID, "123456");

                verify(userPersistencePort).save(any(IdentityUser.class));
                verify(userOtpChallengeStore).delete(USER_ID, UserOtpPurpose.VERIFY_PRIMARY_EMAIL);
        }

        @Test
        void confirmVerifyPrimaryEmailOtpThrowsWhenChallengeMissing() {
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
                when(userOtpChallengeStore.find(USER_ID, UserOtpPurpose.VERIFY_PRIMARY_EMAIL))
                                .thenReturn(Optional.empty());

                assertErrorCode(IdentityErrorCode.EMAIL_VERIFICATION_NOT_FOUND,
                                () -> service.confirmVerifyPrimaryEmailOtp(USER_ID, "123456"));
        }

        @Test
        void confirmVerifyPrimaryEmailOtpRejectsBlankCode() {
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
                when(userOtpChallengeStore.find(USER_ID, UserOtpPurpose.VERIFY_PRIMARY_EMAIL)).thenReturn(
                                Optional.of(challenge(UserOtpPurpose.VERIFY_PRIMARY_EMAIL, OtpChannel.EMAIL,
                                                "u@example.com", "123456", null, future(), 0)));

                assertErrorCode(IdentityErrorCode.OTP_REQUIRED,
                                () -> service.confirmVerifyPrimaryEmailOtp(USER_ID, "  "));
        }

        @Test
        void confirmVerifyPrimaryEmailOtpRejectsExpiredChallenge() {
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
                when(userOtpChallengeStore.find(USER_ID, UserOtpPurpose.VERIFY_PRIMARY_EMAIL)).thenReturn(
                                Optional.of(challenge(UserOtpPurpose.VERIFY_PRIMARY_EMAIL, OtpChannel.EMAIL,
                                                "u@example.com", "123456", null, past(), 0)));

                assertErrorCode(IdentityErrorCode.OTP_EXPIRED,
                                () -> service.confirmVerifyPrimaryEmailOtp(USER_ID, "123456"));

                verify(userOtpChallengeStore).delete(USER_ID, UserOtpPurpose.VERIFY_PRIMARY_EMAIL);
        }

        @Test
        void confirmVerifyPrimaryEmailOtpRejectsWrongCodeAndIncrementsAttempts() {
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
                when(userOtpChallengeStore.find(USER_ID, UserOtpPurpose.VERIFY_PRIMARY_EMAIL)).thenReturn(
                                Optional.of(challenge(UserOtpPurpose.VERIFY_PRIMARY_EMAIL, OtpChannel.EMAIL,
                                                "u@example.com", "123456", null, future(), 0)));
                when(accountManagementPolicy.getOtpMaxAttempts()).thenReturn(5);

                assertErrorCode(IdentityErrorCode.OTP_INVALID,
                                () -> service.confirmVerifyPrimaryEmailOtp(USER_ID, "000000"));

                verify(userOtpChallengeStore).save(any(UserOtpChallenge.class));
        }

        @Test
        void confirmVerifyPrimaryEmailOtpFailsWhenAttemptsExceeded() {
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
                when(userOtpChallengeStore.find(USER_ID, UserOtpPurpose.VERIFY_PRIMARY_EMAIL)).thenReturn(
                                Optional.of(challenge(UserOtpPurpose.VERIFY_PRIMARY_EMAIL, OtpChannel.EMAIL,
                                                "u@example.com", "123456", null, future(), 4)));
                when(accountManagementPolicy.getOtpMaxAttempts()).thenReturn(5);

                assertErrorCode(IdentityErrorCode.OTP_ATTEMPTS_EXCEEDED,
                                () -> service.confirmVerifyPrimaryEmailOtp(USER_ID, "000000"));

                verify(userOtpChallengeStore).delete(USER_ID, UserOtpPurpose.VERIFY_PRIMARY_EMAIL);
        }

        @Test
        void confirmVerifyPrimaryEmailOtpFailsWhenAtomicAttemptBudgetIsExhausted() {
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
                when(userOtpChallengeStore.find(USER_ID, UserOtpPurpose.VERIFY_PRIMARY_EMAIL)).thenReturn(
                                Optional.of(challenge(UserOtpPurpose.VERIFY_PRIMARY_EMAIL, OtpChannel.EMAIL,
                                                "u@example.com", "123456", null, future(), 0)));
                when(accountManagementPolicy.getOtpMaxAttempts()).thenReturn(5);
                when(userOtpChallengeStore.tryAcquireAttempt(
                                eq(USER_ID), eq(UserOtpPurpose.VERIFY_PRIMARY_EMAIL), eq(5), any()))
                                .thenReturn(false);

                assertErrorCode(IdentityErrorCode.OTP_ATTEMPTS_EXCEEDED,
                                () -> service.confirmVerifyPrimaryEmailOtp(USER_ID, "123456"));

                verify(userOtpChallengeStore).delete(USER_ID, UserOtpPurpose.VERIFY_PRIMARY_EMAIL);
        }

        @Test
        void requestEmailChangeOtpSendsToNewEmail() {
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
                when(userPersistencePort.findByIdentity("new@example.com")).thenReturn(Optional.empty());
                when(accountManagementPolicy.getOtpExpirySeconds()).thenReturn(300);

                service.requestEmailChangeOtp(USER_ID, "new@example.com");

                verify(userOtpChallengeStore).save(any(UserOtpChallenge.class));
                verify(notificationPort).sendEmailOtp(eq("new@example.com"), anyString());
        }

        @Test
        void requestEmailChangeOtpRejectsBlankEmail() {
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));

                assertErrorCode(IdentityErrorCode.EMAIL_REQUIRED,
                                () -> service.requestEmailChangeOtp(USER_ID, " "));
        }

        @Test
        void requestEmailChangeOtpRejectsEmailUsedByAnotherUser() {
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
                when(userPersistencePort.findByIdentity("new@example.com")).thenReturn(
                                Optional.of(IdentityUser.createNew("other", "new@example.com", null, "other", TestClocks.FIXED)));

                assertErrorCode(IdentityErrorCode.EMAIL_ALREADY_EXISTS,
                                () -> service.requestEmailChangeOtp(USER_ID, "new@example.com"));
        }

        @Test
        void confirmEmailChangeUpdatesEmailRevokesSessionsAndPublishesEvent() {
                UserProfileView expected = profileView();
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
                when(userOtpChallengeStore.find(USER_ID, UserOtpPurpose.CHANGE_EMAIL)).thenReturn(
                                Optional.of(challenge(UserOtpPurpose.CHANGE_EMAIL, OtpChannel.EMAIL,
                                                "new@example.com", "123456", "new@example.com", future(), 0)));
                when(userPersistencePort.findByIdentity("new@example.com")).thenReturn(Optional.empty());
                when(userPersistencePort.save(any(IdentityUser.class))).thenAnswer(inv -> inv.getArgument(0));
                when(authSessionPersistencePort.findByUserId(USER_ID)).thenReturn(List.of(activeSession()));
                when(userResultMapper.toUserProfileView(any(IdentityUser.class))).thenReturn(expected);

                UserProfileView result = service.confirmEmailChange(USER_ID, "123456");

                assertThat(result).isSameAs(expected);
                verify(refreshTokenStore).revokeBySessionId("session-1");
                verify(userOtpChallengeStore).delete(USER_ID, UserOtpPurpose.CHANGE_EMAIL);
                verify(integrationEventPublisher).publishEmailChanged(eq(USER_ID), eq("u@example.com"),
                                eq("new@example.com"));
        }

        @Test
        void confirmEmailChangeThrowsWhenChallengeMissing() {
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
                when(userOtpChallengeStore.find(USER_ID, UserOtpPurpose.CHANGE_EMAIL)).thenReturn(Optional.empty());

                assertErrorCode(IdentityErrorCode.EMAIL_CHANGE_NOT_FOUND,
                                () -> service.confirmEmailChange(USER_ID, "123456"));
        }

        @Test
        void confirmEmailChangeRejectsEmailTakenAtConfirmTime() {
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
                when(userOtpChallengeStore.find(USER_ID, UserOtpPurpose.CHANGE_EMAIL)).thenReturn(
                                Optional.of(challenge(UserOtpPurpose.CHANGE_EMAIL, OtpChannel.EMAIL,
                                                "new@example.com", "123456", "new@example.com", future(), 0)));
                when(userPersistencePort.findByIdentity("new@example.com")).thenReturn(
                                Optional.of(IdentityUser.createNew("other", "new@example.com", null, "other", TestClocks.FIXED)));

                assertErrorCode(IdentityErrorCode.EMAIL_ALREADY_EXISTS,
                                () -> service.confirmEmailChange(USER_ID, "123456"));
        }

        @Test
        void requestPhoneChangeOtpSendsToNewPhone() {
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
                when(userPersistencePort.existsByPhone("+15550001")).thenReturn(false);
                when(accountManagementPolicy.getOtpExpirySeconds()).thenReturn(300);

                service.requestPhoneChangeOtp(USER_ID, "+15550001");

                verify(userOtpChallengeStore).save(any(UserOtpChallenge.class));
                verify(notificationPort).sendPhoneOtp(eq("+15550001"), anyString());
        }

        @Test
        void requestPhoneChangeOtpRejectsBlankPhone() {
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));

                assertErrorCode(IdentityErrorCode.PHONE_REQUIRED,
                                () -> service.requestPhoneChangeOtp(USER_ID, ""));
        }

        @Test
        void requestPhoneChangeOtpRejectsExistingPhone() {
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
                when(userPersistencePort.existsByPhone("+15550001")).thenReturn(true);

                assertErrorCode(IdentityErrorCode.PHONE_ALREADY_EXISTS,
                                () -> service.requestPhoneChangeOtp(USER_ID, "+15550001"));
        }

        @Test
        void confirmPhoneChangeUpdatesPhoneAndPublishesEvent() {
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
                when(userOtpChallengeStore.find(USER_ID, UserOtpPurpose.CHANGE_PHONE)).thenReturn(
                                Optional.of(challenge(UserOtpPurpose.CHANGE_PHONE, OtpChannel.PHONE,
                                                "+15550001", "123456", "+15550001", future(), 0)));
                when(userPersistencePort.existsByPhone("+15550001")).thenReturn(false);
                when(userPersistencePort.save(any(IdentityUser.class))).thenAnswer(inv -> inv.getArgument(0));
                when(authSessionPersistencePort.findByUserId(USER_ID)).thenReturn(List.of(activeSession()));
                when(userResultMapper.toUserProfileView(any(IdentityUser.class))).thenReturn(profileView());

                service.confirmPhoneChange(USER_ID, "123456");

                verify(refreshTokenStore).revokeBySessionId("session-1");
                verify(userOtpChallengeStore).delete(USER_ID, UserOtpPurpose.CHANGE_PHONE);
                verify(integrationEventPublisher).publishPhoneChanged(eq(USER_ID), eq(null), eq("+15550001"));
        }

        @Test
        void confirmPhoneChangeRejectsPhoneTakenByAnotherUser() {
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
                when(userOtpChallengeStore.find(USER_ID, UserOtpPurpose.CHANGE_PHONE)).thenReturn(
                                Optional.of(challenge(UserOtpPurpose.CHANGE_PHONE, OtpChannel.PHONE,
                                                "+15550001", "123456", "+15550001", future(), 0)));
                when(userPersistencePort.existsByPhone("+15550001")).thenReturn(true);

                assertErrorCode(IdentityErrorCode.PHONE_ALREADY_EXISTS,
                                () -> service.confirmPhoneChange(USER_ID, "123456"));
        }

        @Test
        void requestAccountDeletionRejectsInvalidPassword() {
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
                when(userSecurityPort.findById(USER_ID)).thenReturn(Optional.of(
                                new UserSecurityPort.UserSecurityData(USER_ID, "hash", UserStatus.ACTIVE, false, null, null, 0)));
                when(passwordHasher.matches("wrong", "hash")).thenReturn(false);

                assertErrorCode(IdentityErrorCode.INVALID_CREDENTIALS,
                                () -> service.requestAccountDeletion(
                                                new RequestAccountDeletionCommand(USER_ID, "wrong", null)));
        }

        @Test
        void requestAccountDeletionRejectsMissingMfaCode() {
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
                when(userSecurityPort.findById(USER_ID)).thenReturn(Optional.of(
                                new UserSecurityPort.UserSecurityData(USER_ID, null, UserStatus.ACTIVE, true, "secret", null, 0)));

                assertErrorCode(IdentityErrorCode.OTP_REQUIRED,
                                () -> service.requestAccountDeletion(
                                                new RequestAccountDeletionCommand(USER_ID, null, " ")));
        }

        @Test
        void requestAccountDeletionAcceptsValidBackupCode() {
                DeletionRequestView view = new DeletionRequestView(
                                "req-mfa", "PENDING", FIXED_NOW, FIXED_NOW.plus(Duration.ofDays(30)));
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
                when(userSecurityPort.findById(USER_ID)).thenReturn(Optional.of(
                                new UserSecurityPort.UserSecurityData(USER_ID, null, UserStatus.ACTIVE, true, "secret", null, 0)));
                when(totpManager.verifyCode("secret", "backup")).thenReturn(false);
                when(mfaPersistencePort.findActiveBackupCodes(USER_ID))
                                .thenReturn(List.of(new MfaPersistencePort.BackupCodeData("backup-1", "hash")));
                when(passwordHasher.matches("backup", "hash")).thenReturn(true);
                when(mfaPersistencePort.markBackupCodeUsed(eq("backup-1"), any(Instant.class))).thenReturn(true);
                when(orderQueryPort.hasOpenOrdersForUser(USER_ID)).thenReturn(false);
                when(merchantQueryPort.findMerchantIdByOwnerId(USER_ID)).thenReturn(Optional.empty());
                when(accountDeletionPort.findPendingByUserId(USER_ID)).thenReturn(Optional.empty());
                when(accountManagementPolicy.getDeletionGraceDays()).thenReturn(30);
                when(accountDeletionPort.save(eq(USER_ID), any(Instant.class))).thenReturn(view);

                assertThat(service.requestAccountDeletion(
                                new RequestAccountDeletionCommand(USER_ID, null, "backup"))).isSameAs(view);
        }

        @Test
        void requestAccountDeletionRejectsInvalidMfaCode() {
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
                when(userSecurityPort.findById(USER_ID)).thenReturn(Optional.of(
                                new UserSecurityPort.UserSecurityData(USER_ID, null, UserStatus.ACTIVE, true, "secret", null, 0)));
                when(totpManager.verifyCode("secret", "bad")).thenReturn(false);
                when(mfaPersistencePort.findActiveBackupCodes(USER_ID)).thenReturn(List.of());

                assertErrorCode(IdentityErrorCode.OTP_INVALID,
                                () -> service.requestAccountDeletion(
                                                new RequestAccountDeletionCommand(USER_ID, null, "bad")));
        }

        @Test
        void requestAccountDeletionRejectsWhenMerchantHasOpenOrders() {
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
                when(userSecurityPort.findById(USER_ID)).thenReturn(Optional.of(
                                new UserSecurityPort.UserSecurityData(USER_ID, null, UserStatus.ACTIVE, false, null, null, 0)));
                when(orderQueryPort.hasOpenOrdersForUser(USER_ID)).thenReturn(false);
                when(merchantQueryPort.findMerchantIdByOwnerId(USER_ID)).thenReturn(Optional.of("merchant-1"));
                when(orderQueryPort.hasOpenOrdersForMerchant("merchant-1")).thenReturn(true);

                assertErrorCode(IdentityErrorCode.ACCOUNT_HAS_OPEN_ORDERS,
                                () -> service.requestAccountDeletion(new RequestAccountDeletionCommand(USER_ID)));
        }

        @Test
        void sendingAVerificationOtpRequiresAnEmailAddress() {
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(
                                IdentityUser.createNew(USER_ID, null, "+84900000000", "user", TestClocks.FIXED)));

                assertErrorCode(IdentityErrorCode.EMAIL_REQUIRED,
                                () -> service.sendVerifyPrimaryEmailOtp(USER_ID));
                verify(userOtpChallengeStore, never()).save(any());
        }

        @Test
        void requestingAnEmailChangeRejectsABlankAddress() {
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));

                assertErrorCode(IdentityErrorCode.EMAIL_REQUIRED,
                                () -> service.requestEmailChangeOtp(USER_ID, " "));
        }

        @Test
        void requestingAnEmailChangeRejectsAnAddressOwnedByAnotherUser() {
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
                when(userPersistencePort.findByIdentity("other@example.com")).thenReturn(Optional.of(
                                IdentityUser.createNew("other", "other@example.com", null, "other",
                                                TestClocks.FIXED)));

                assertErrorCode(IdentityErrorCode.EMAIL_ALREADY_EXISTS,
                                () -> service.requestEmailChangeOtp(USER_ID, "other@example.com"));
                verify(userOtpChallengeStore, never()).save(any());
        }

        @Test
        void requestingAPhoneChangeRejectsABlankNumber() {
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));

                assertErrorCode(IdentityErrorCode.PHONE_REQUIRED,
                                () -> service.requestPhoneChangeOtp(USER_ID, " "));
        }

        @Test
        void confirmingAnEmailChangeTranslatesAUniqueConstraintRace() {
                IdentityUser user = activeUser();
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(user));
                when(userOtpChallengeStore.find(USER_ID, UserOtpPurpose.CHANGE_EMAIL))
                                .thenReturn(Optional.of(new UserOtpChallenge(USER_ID,
                                                UserOtpPurpose.CHANGE_EMAIL, OtpChannel.EMAIL,
                                                "u@example.com", "123456", "new@example.com",
                                                FIXED_NOW.plus(Duration.ofMinutes(5)), 0)));
                when(userPersistencePort.findByIdentity("new@example.com")).thenReturn(Optional.empty());
                when(userPersistencePort.save(user))
                                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("dup"));

                assertErrorCode(IdentityErrorCode.EMAIL_ALREADY_EXISTS,
                                () -> service.confirmEmailChange(USER_ID, "123456"));
        }

        @Test
        void aMissingDataExportPayloadIsReported() {
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
                when(dataExportPort.findPayload("missing", USER_ID)).thenReturn(Optional.empty());

                assertErrorCode(IdentityErrorCode.DATA_EXPORT_NOT_FOUND,
                                () -> service.getDataExport(USER_ID, "missing"));
        }
}