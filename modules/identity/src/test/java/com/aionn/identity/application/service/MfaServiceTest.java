package com.aionn.identity.application.service;


import java.time.Clock;
import com.aionn.identity.application.dto.security.result.MfaResult;
import com.aionn.identity.application.dto.security.result.MfaSetupResult;
import com.aionn.identity.application.policy.MfaPolicy;
import com.aionn.identity.application.port.out.security.MfaPersistencePort;
import com.aionn.identity.application.port.out.security.PasswordHasherPort;
import com.aionn.identity.application.port.out.security.SecurityAuditPort;
import com.aionn.identity.application.port.out.security.TotpManagerPort;
import com.aionn.identity.application.port.out.security.UserSecurityPort;
import com.aionn.identity.application.port.out.observability.IdentityMetricsPort;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.valueobject.SecurityAuditEventType;
import com.aionn.identity.domain.valueobject.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class MfaServiceTest {

        private static final String USER_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";

        @Mock
        private UserSecurityPort userSecurityPort;
        @Mock
        private MfaPersistencePort mfaPersistencePort;
        @Mock
        private SecurityAuditPort securityAuditPort;
        @Mock
        private PasswordHasherPort passwordHasher;
        @Mock
        private TotpManagerPort totpManager;
        @Mock
        private MfaPolicy mfaPolicy;
        @Mock
        private IdentityMetricsPort identityMetrics;

        private MfaService mfaService;

        @BeforeEach
        void setUp() {
                mfaService = new MfaService(
                                userSecurityPort,
                                mfaPersistencePort,
                                securityAuditPort,
                                passwordHasher,
                                totpManager,
                                mfaPolicy,
                                identityMetrics,
                                Clock.systemUTC());
        }

        @Test
        void initiateSetupReturnsSecretAndUriWhenPasswordValid() {
                var data = new UserSecurityPort.UserSecurityData(
                                USER_ID, "hash", UserStatus.ACTIVE, false, null, null, 0);
                when(userSecurityPort.findById(USER_ID)).thenReturn(Optional.of(data));
                when(passwordHasher.matches("pwd", "hash")).thenReturn(true);
                when(totpManager.generateSecret()).thenReturn("SECRET");
                when(totpManager.buildOtpAuthUri("Aionn", USER_ID, "SECRET")).thenReturn("otpauth://uri");
                when(mfaPolicy.getMfaIssuer()).thenReturn("Aionn");

                MfaSetupResult result = mfaService.initiateSetup(USER_ID, "pwd", "1.1.1.1");

                assertThat(result.secret()).isEqualTo("SECRET");
                assertThat(result.otpauthUri()).isEqualTo("otpauth://uri");
                assertThat(result.issuer()).isEqualTo("Aionn");
                assertThat(result.accountName()).isEqualTo(USER_ID);
                verify(mfaPersistencePort).saveMfaSecret(USER_ID, "SECRET");
                verify(mfaPersistencePort).updateMfaStatus(USER_ID, false);
                verify(mfaPersistencePort).deleteBackupCodes(USER_ID);
                verify(securityAuditPort).saveAuditLog(USER_ID,
                                SecurityAuditEventType.MFA_SETUP_INITIATED, "1.1.1.1");
        }

        @Test
        void initiateSetupRejectsWhenMfaAlreadyEnabled() {
                var data = new UserSecurityPort.UserSecurityData(
                                USER_ID, "hash", UserStatus.ACTIVE, true, "SECRET", null, 0);
                when(userSecurityPort.findById(USER_ID)).thenReturn(Optional.of(data));
                when(passwordHasher.matches("pwd", "hash")).thenReturn(true);

                var ex = assertThrows(IdentityException.class,
                                () -> mfaService.initiateSetup(USER_ID, "pwd", null));

                assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.MFA_ALREADY_ENABLED.getCode());
                verify(mfaPersistencePort, never()).saveMfaSecret(anyString(), anyString());
        }

        @Test
        void initiateSetupRejectsWrongPassword() {
                var data = new UserSecurityPort.UserSecurityData(
                                USER_ID, "hash", UserStatus.ACTIVE, false, null, null, 0);
                when(userSecurityPort.findById(USER_ID)).thenReturn(Optional.of(data));
                when(passwordHasher.matches("pwd", "hash")).thenReturn(false);

                var ex = assertThrows(IdentityException.class,
                                () -> mfaService.initiateSetup(USER_ID, "pwd", null));

                assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.INVALID_CREDENTIALS.getCode());
        }

        @Test
        void enableMfaPersistsBackupCodesAndEnablesFlag() {
                var data = new UserSecurityPort.UserSecurityData(
                                USER_ID, "hash", UserStatus.ACTIVE, false, "SECRET", null, 0);
                when(userSecurityPort.findById(USER_ID)).thenReturn(Optional.of(data));
                when(passwordHasher.matches("pwd", "hash")).thenReturn(true);
                when(totpManager.verifyCode("SECRET", "123456")).thenReturn(true);
                when(mfaPolicy.getBackupCodeCount()).thenReturn(8);
                when(passwordHasher.hash(anyString())).thenReturn("hashed");

                MfaResult result = mfaService.enableMfa(USER_ID, "pwd", "123456", "1.1.1.1");

                assertThat(result.mfaEnabled()).isTrue();
                assertThat(result.backupCodes()).isNotNull();
                assertThat(result.backupCodes().size()).isEqualTo(8);
                verify(mfaPersistencePort).updateMfaStatus(USER_ID, true);
                verify(mfaPersistencePort).deleteBackupCodes(USER_ID);
                ArgumentCaptor<List<String>> captor = ArgumentCaptor.captor();
                verify(mfaPersistencePort).saveBackupCodes(eq(USER_ID), captor.capture());
                assertThat(captor.getValue().size()).isEqualTo(8);
                verify(securityAuditPort).saveAuditLog(USER_ID,
                                SecurityAuditEventType.MFA_ENABLED, "1.1.1.1");
        }

        @Test
        void enableMfaRejectsWhenSecretMissing() {
                var data = new UserSecurityPort.UserSecurityData(
                                USER_ID, "hash", UserStatus.ACTIVE, false, null, null, 0);
                when(userSecurityPort.findById(USER_ID)).thenReturn(Optional.of(data));
                when(passwordHasher.matches("pwd", "hash")).thenReturn(true);

                var ex = assertThrows(IdentityException.class,
                                () -> mfaService.enableMfa(USER_ID, "pwd", "123456", null));

                assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.MFA_SETUP_NOT_INITIATED.getCode());
        }

        @Test
        void enableMfaRejectsWrongTotp() {
                var data = new UserSecurityPort.UserSecurityData(
                                USER_ID, "hash", UserStatus.ACTIVE, false, "SECRET", null, 0);
                when(userSecurityPort.findById(USER_ID)).thenReturn(Optional.of(data));
                when(passwordHasher.matches("pwd", "hash")).thenReturn(true);
                when(totpManager.verifyCode("SECRET", "000000")).thenReturn(false);

                var ex = assertThrows(IdentityException.class,
                                () -> mfaService.enableMfa(USER_ID, "pwd", "000000", null));

                assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.OTP_INVALID.getCode());
                verify(mfaPersistencePort, never()).updateMfaStatus(anyString(), eq(true));
        }

        @Test
        void disableMfaClearsMfaWhenTotpValid() {
                var data = new UserSecurityPort.UserSecurityData(
                                USER_ID, "hash", UserStatus.ACTIVE, true, "SECRET", null, 0);
                when(userSecurityPort.findById(USER_ID)).thenReturn(Optional.of(data));
                when(passwordHasher.matches("pwd", "hash")).thenReturn(true);
                when(totpManager.verifyCode("SECRET", "123456")).thenReturn(true);

                MfaResult result = mfaService.disableMfa(USER_ID, "pwd", "123456", null);

                assertThat(result.mfaEnabled()).isFalse();
                assertThat(result.backupCodes()).isNull();
                verify(mfaPersistencePort).clearMfa(USER_ID);
                verify(securityAuditPort).saveAuditLog(USER_ID,
                                SecurityAuditEventType.MFA_DISABLED, null);
        }

        @Test
        void disableMfaUsesBackupCodeWhenTotpFails() {
                var data = new UserSecurityPort.UserSecurityData(
                                USER_ID, "hash", UserStatus.ACTIVE, true, "SECRET", null, 0);
                when(userSecurityPort.findById(USER_ID)).thenReturn(Optional.of(data));
                when(passwordHasher.matches("pwd", "hash")).thenReturn(true);
                when(totpManager.verifyCode("SECRET", "00000000")).thenReturn(false);
                var backup = new MfaPersistencePort.BackupCodeData("bc-1", "hashed");
                when(mfaPersistencePort.findActiveBackupCodes(USER_ID)).thenReturn(List.of(backup));
                when(passwordHasher.matches("00000000", "hashed")).thenReturn(true);
                when(mfaPersistencePort.markBackupCodeUsed(eq("bc-1"), any())).thenReturn(true);

                MfaResult result = mfaService.disableMfa(USER_ID, "pwd", "00000000", null);

                assertThat(result.mfaEnabled()).isFalse();
                verify(mfaPersistencePort).markBackupCodeUsed(eq("bc-1"), any());
                verify(mfaPersistencePort).clearMfa(USER_ID);
        }

        @Test
        void disableMfaRejectsWhenMfaNotEnabled() {
                var data = new UserSecurityPort.UserSecurityData(
                                USER_ID, "hash", UserStatus.ACTIVE, false, null, null, 0);
                when(userSecurityPort.findById(USER_ID)).thenReturn(Optional.of(data));
                when(passwordHasher.matches("pwd", "hash")).thenReturn(true);

                var ex = assertThrows(IdentityException.class,
                                () -> mfaService.disableMfa(USER_ID, "pwd", "123456", null));

                assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.MFA_NOT_ENABLED.getCode());
        }

        @Test
        void regenerateBackupCodesReplacesExistingCodes() {
                var data = new UserSecurityPort.UserSecurityData(
                                USER_ID, "hash", UserStatus.ACTIVE, true, "SECRET", null, 0);
                when(userSecurityPort.findById(USER_ID)).thenReturn(Optional.of(data));
                when(passwordHasher.matches("pwd", "hash")).thenReturn(true);
                when(totpManager.verifyCode("SECRET", "123456")).thenReturn(true);
                when(mfaPolicy.getBackupCodeCount()).thenReturn(8);
                when(passwordHasher.hash(anyString())).thenReturn("hashed");

                List<String> codes = mfaService.regenerateBackupCodes(USER_ID, "pwd", "123456", null);

                assertThat(codes.size()).isEqualTo(8);
                verify(mfaPersistencePort).deleteBackupCodes(USER_ID);
                verify(mfaPersistencePort).saveBackupCodes(eq(USER_ID), anyList());
                verify(securityAuditPort).saveAuditLog(USER_ID,
                                SecurityAuditEventType.MFA_BACKUP_CODES_REGENERATED, null);
        }

        @Test
        void operationsRejectMissingUser() {
                when(userSecurityPort.findById(USER_ID)).thenReturn(Optional.empty());

                var ex = assertThrows(IdentityException.class,
                                () -> mfaService.initiateSetup(USER_ID, "pwd", null));

                assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.USER_NOT_FOUND.getCode());
        }
}
