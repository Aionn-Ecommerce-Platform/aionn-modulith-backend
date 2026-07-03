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
import com.aionn.identity.application.port.out.user.UserPersistencePort;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.model.IdentityUser;
import com.aionn.sharedkernel.integration.port.notification.IdentityNotificationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountManagementServiceTest {

    private static final String USER_ID = "user-1";

    @Mock private UserPersistencePort userPersistencePort;
    @Mock private IdentityNotificationPort notificationPort;
    @Mock private IdentityIntegrationEventPublisherPort integrationEventPublisher;
    @Mock private UserOtpChallengeStorePort userOtpChallengeStore;
    @Mock private AccountDeletionPort accountDeletionPort;
    @Mock private DataExportPort dataExportPort;
    @Mock private AuthSessionPersistencePort authSessionPersistencePort;
    @Mock private RefreshTokenStorePort refreshTokenStore;
    @Mock private AccountManagementPolicy accountManagementPolicy;
    @Mock private UserResultMapper userResultMapper;

    private AccountManagementService service;

    @BeforeEach
    void setUp() {
        service = new AccountManagementService(
                userPersistencePort, notificationPort, integrationEventPublisher,
                userOtpChallengeStore, accountDeletionPort, dataExportPort,
                authSessionPersistencePort, refreshTokenStore,
                accountManagementPolicy, userResultMapper);
    }

    private static IdentityUser activeUser() {
        return IdentityUser.createNew(USER_ID, "u@example.com", null, "user");
    }

    @Test
    void requestAccountDeletionPersistsAndReturnsView() {
        DeletionRequestView view = new DeletionRequestView(
                "req-1", "PENDING", LocalDateTime.now(), LocalDateTime.now().plusDays(30));
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(accountDeletionPort.findPendingByUserId(USER_ID)).thenReturn(Optional.empty());
        when(accountManagementPolicy.getDeletionGraceDays()).thenReturn(30);
        when(accountDeletionPort.save(eq(USER_ID), any(LocalDateTime.class))).thenReturn(view);

        DeletionRequestView result = service.requestAccountDeletion(
                new RequestAccountDeletionCommand(USER_ID));

        assertSame(view, result);
        verify(accountDeletionPort).save(eq(USER_ID), any(LocalDateTime.class));
    }

    @Test
    void requestAccountDeletionRejectsWhenAlreadyPending() {
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(accountDeletionPort.findPendingByUserId(USER_ID))
                .thenReturn(Optional.of(new DeletionRequestView(
                        "req-old", "PENDING", LocalDateTime.now(), LocalDateTime.now().plusDays(10))));

        assertThrows(IdentityException.class,
                () -> service.requestAccountDeletion(new RequestAccountDeletionCommand(USER_ID)));

        verify(accountDeletionPort, never()).save(any(), any());
    }

    @Test
    void cancelAccountDeletionDelegatesToPort() {
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(accountDeletionPort.findPendingByUserId(USER_ID)).thenReturn(Optional.of(
                new DeletionRequestView("r", "PENDING", LocalDateTime.now(), LocalDateTime.now())));

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
                "exp-1", "PENDING", LocalDateTime.now());
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(dataExportPort.hasActiveRequest(USER_ID)).thenReturn(false);
        when(dataExportPort.save(USER_ID)).thenReturn(view);

        DataExportRequestView result = service.requestDataExport(
                new RequestDataExportCommand(USER_ID));

        assertSame(view, result);
    }
}
