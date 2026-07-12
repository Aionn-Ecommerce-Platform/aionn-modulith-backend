package com.aionn.identity.application.service;

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
import com.aionn.sharedkernel.integration.port.notification.IdentityNotificationPort;
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

        private AccountManagementService service;

        @BeforeEach
        void setUp() {
                service = new AccountManagementService(
                                userPersistencePort, notificationPort, integrationEventPublisher,
                                userOtpChallengeStore, accountDeletionPort, dataExportPort,
                                authSessionPersistencePort, refreshTokenStore,
                                accountManagementPolicy, userResultMapper,
                                Clock.systemUTC());
        }

        private static IdentityUser activeUser() {
                return IdentityUser.createNew(USER_ID, "u@example.com", null, "user");
        }

        @Test
        void requestAccountDeletionPersistsAndReturnsView() {
                DeletionRequestView view = new DeletionRequestView(
                                "req-1", "PENDING", Instant.now(), Instant.now().plus(Duration.ofDays(30)));
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
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
                when(accountDeletionPort.findPendingByUserId(USER_ID))
                                .thenReturn(Optional.of(new DeletionRequestView(
                                                "req-old", "PENDING", Instant.now(),
                                                Instant.now().plus(Duration.ofDays(10)))));

                assertThrows(IdentityException.class,
                                () -> service.requestAccountDeletion(new RequestAccountDeletionCommand(USER_ID)));

                verify(accountDeletionPort, never()).save(any(), any());
        }

        @Test
        void cancelAccountDeletionDelegatesToPort() {
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
                when(accountDeletionPort.findPendingByUserId(USER_ID)).thenReturn(Optional.of(
                                new DeletionRequestView("r", "PENDING", Instant.now(), Instant.now())));

                service.cancelAccountDeletion(new CancelAccountDeletionCommand(USER_ID));

                verify(accountDeletionPort).cancel(USER_ID);
        }

        @Test
        void cancelAccountDeletionThrowsWhenNothingToCancel() {
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
                when(accountDeletionPort.findPendingByUserId(USER_ID)).thenReturn(Optional.empty());

                assertThrows(IdentityException.class,
                                () -> service.cancelAccountDeletion(new CancelAccountDeletionCommand(USER_ID)));
        }

        @Test
        void requestDataExportRejectsWhenAlreadyInProgress() {
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
                when(dataExportPort.hasActiveRequest(USER_ID)).thenReturn(true);

                assertThrows(IdentityException.class,
                                () -> service.requestDataExport(new RequestDataExportCommand(USER_ID)));
        }

        @Test
        void requestDataExportPersistsAndReturnsView() {
                DataExportRequestView view = new DataExportRequestView(
                                "exp-1", "PENDING", Instant.now());
                when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
                when(dataExportPort.hasActiveRequest(USER_ID)).thenReturn(false);
                when(dataExportPort.save(USER_ID)).thenReturn(view);

                DataExportRequestView result = service.requestDataExport(
                                new RequestDataExportCommand(USER_ID));

                assertThat(result).isSameAs(view);
        }

        private static IdentityUser bannedUser() {
                IdentityUser user = IdentityUser.createNew(USER_ID, "u@example.com", null, "user");
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
                return Instant.now(Clock.systemUTC()).plus(Duration.ofDays(1));
        }

        private static Instant past() {
                return Instant.now(Clock.systemUTC()).minus(Duration.ofDays(1));
        }

        private static AuthSession activeSession() {
                return AuthSession.createNew("session-1", USER_ID, "ip", "ua",
                                Instant.now(Clock.systemUTC()).plus(Duration.ofDays(7)));
        }

        private static UserProfileView profileView() {
                return new UserProfileView(USER_ID, "u@example.com", null, "user", null, null,
                                Set.of("BUYER"), "ACTIVE", null, null, Instant.now());
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
                                .thenReturn(Optional.of(IdentityUser.createNew(USER_ID, null, null, "user")));

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
                                Optional.of(IdentityUser.createNew("other", "new@example.com", null, "other")));

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
                                Optional.of(IdentityUser.createNew("other", "new@example.com", null, "other")));

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
}
