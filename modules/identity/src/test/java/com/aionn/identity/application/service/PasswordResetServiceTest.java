package com.aionn.identity.application.service;

import com.aionn.identity.application.policy.AuthPolicy;
import com.aionn.identity.application.port.out.auth.AuthSessionPersistencePort;
import com.aionn.identity.application.port.out.auth.RefreshTokenStorePort;
import com.aionn.identity.application.port.out.integration.IdentityIntegrationEventPublisherPort;
import com.aionn.identity.application.port.out.observability.IdentityMetricsPort;
import com.aionn.identity.application.port.out.security.PasswordHasherPort;
import com.aionn.identity.application.port.out.security.PasswordResetPort;
import com.aionn.identity.application.port.out.security.SecurityAuditPort;
import com.aionn.identity.application.port.out.security.UserSecurityPort;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.valueobject.SecurityAuditEventType;
import com.aionn.identity.domain.valueobject.UserStatus;
import com.aionn.sharedkernel.integration.port.notification.IdentityNotificationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private static final String USER_ID = "user-1";
    private static final String IP = "127.0.0.1";

    @Mock private UserSecurityPort userSecurityPort;
    @Mock private PasswordResetPort passwordResetPort;
    @Mock private SecurityAuditPort securityAuditPort;
    @Mock private PasswordHasherPort passwordHasher;
    @Mock private AuthSessionPersistencePort authSessionPersistencePort;
    @Mock private RefreshTokenStorePort refreshTokenStore;
    @Mock private IdentityNotificationPort notificationPort;
    @Mock private IdentityIntegrationEventPublisherPort integrationEventPublisher;
    @Mock private IdentityMetricsPort identityMetrics;
    @Mock private AuthPolicy authPolicy;

    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(
                userSecurityPort, passwordResetPort, securityAuditPort, passwordHasher,
                authSessionPersistencePort, refreshTokenStore, notificationPort,
                integrationEventPublisher, identityMetrics, authPolicy,
                Clock.systemUTC());
    }

    private static UserSecurityPort.UserSecurityData userSecurity() {
        return new UserSecurityPort.UserSecurityData(
                USER_ID, "hashed-password", UserStatus.ACTIVE, false, null, null, 0);
    }

    @Test
    void changePasswordHashesAndPersistsNewPassword() {
        when(userSecurityPort.findById(USER_ID)).thenReturn(Optional.of(userSecurity()));
        when(passwordHasher.matches("OldPass1", "hashed-password")).thenReturn(true);
        when(passwordHasher.hash("NewPass2")).thenReturn("new-hashed");
        when(authSessionPersistencePort.findByUserId(USER_ID)).thenReturn(List.of());

        service.changePassword(USER_ID, "OldPass1", "NewPass2", IP);

        verify(passwordResetPort).updatePassword(USER_ID, "new-hashed");
        verify(securityAuditPort).saveAuditLog(USER_ID,
                SecurityAuditEventType.PASSWORD_CHANGED, IP);
        verify(integrationEventPublisher).publishPasswordChanged(USER_ID, "self-service");
    }

    @Test
    void changePasswordRejectsSameAsCurrent() {
        assertThrows(IdentityException.class,
                () -> service.changePassword(USER_ID, "SamePass1", "SamePass1", IP));

        verify(passwordResetPort, never()).updatePassword(anyString(), anyString());
    }

    @Test
    void changePasswordRejectsInvalidCurrentPassword() {
        when(userSecurityPort.findById(USER_ID)).thenReturn(Optional.of(userSecurity()));
        when(passwordHasher.matches("WrongOld1", "hashed-password")).thenReturn(false);

        assertThrows(IdentityException.class,
                () -> service.changePassword(USER_ID, "WrongOld1", "NewPass2", IP));

        verify(passwordResetPort, never()).updatePassword(anyString(), anyString());
    }

    @Test
    void changePasswordRejectsMissingCurrentPassword() {
        assertThrows(IdentityException.class,
                () -> service.changePassword(USER_ID, null, "NewPass2", IP));

        verify(passwordResetPort, never()).updatePassword(anyString(), anyString());
    }

    @Test
    void requestPasswordResetSilentlyIgnoresUnknownIdentity() {
        when(userSecurityPort.findByIdentity("ghost@example.com")).thenReturn(Optional.empty());

        service.requestPasswordReset("ghost@example.com", IP);

        verify(passwordResetPort, never()).savePasswordResetTokenHash(any(), any(), any());
        verify(notificationPort, never()).sendPasswordResetRequested(any(), any());
    }

    @Test
    void requestPasswordResetIssuesTokenAndSendsNotification() {
        when(userSecurityPort.findByIdentity("u@example.com"))
                .thenReturn(Optional.of(userSecurity()));
        when(authPolicy.getPasswordResetTokenTtlMinutes()).thenReturn(30);

        service.requestPasswordReset("u@example.com", IP);

        verify(passwordResetPort).savePasswordResetTokenHash(anyString(), eq(USER_ID),
                any(Instant.class));
        verify(notificationPort).sendPasswordResetRequested(eq(USER_ID), anyString());
        verify(securityAuditPort).saveAuditLog(USER_ID,
                SecurityAuditEventType.PASSWORD_RESET_REQUESTED, IP);
    }

    @Test
    void completePasswordResetRejectsExpiredToken() {
        PasswordResetPort.PasswordResetTokenData expired =
                new PasswordResetPort.PasswordResetTokenData(USER_ID, nowUtc().minus(Duration.ofMinutes(1)));
        when(passwordResetPort.consumePasswordResetTokenHash(anyString()))
                .thenReturn(Optional.of(expired));

        assertThrows(IdentityException.class,
                () -> service.completePasswordReset("token-1", "NewPass2", IP));
    }

    @Test
    void completePasswordResetUpdatesPasswordAndPublishesEvent() {
        PasswordResetPort.PasswordResetTokenData valid =
                new PasswordResetPort.PasswordResetTokenData(USER_ID, nowUtc().plus(Duration.ofMinutes(10)));
        when(passwordResetPort.consumePasswordResetTokenHash(anyString()))
                .thenReturn(Optional.of(valid));
        when(passwordHasher.hash("NewPass2")).thenReturn("new-hash");
        when(authSessionPersistencePort.findByUserId(USER_ID)).thenReturn(List.of());

        service.completePasswordReset("token-2", "NewPass2", IP);

        verify(passwordResetPort).updatePassword(USER_ID, "new-hash");
        verify(integrationEventPublisher).publishPasswordChanged(USER_ID, "password reset");
    }

    private static Instant nowUtc() {
        return Instant.now();
    }
}
