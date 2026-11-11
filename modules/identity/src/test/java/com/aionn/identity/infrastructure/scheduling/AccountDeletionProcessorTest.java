package com.aionn.identity.infrastructure.scheduling;

import com.aionn.identity.application.port.out.kyc.KycDocumentStoragePort;
import com.aionn.identity.domain.valueobject.AccountDeletionStatus;
import com.aionn.identity.domain.valueobject.AuthSessionStatus;
import com.aionn.identity.domain.valueobject.UserStatus;
import com.aionn.identity.infrastructure.config.properties.AccountManagementProperties;
import com.aionn.identity.infrastructure.persistence.entity.AccountDeletionRequestEntity;
import com.aionn.identity.infrastructure.persistence.entity.AuthSessionEntity;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import com.aionn.identity.infrastructure.persistence.repository.account.AccountDeletionRequestRepository;
import com.aionn.identity.infrastructure.persistence.repository.auth.AuthSessionRepository;
import com.aionn.identity.infrastructure.persistence.repository.auth.SocialAccountRepository;
import com.aionn.identity.infrastructure.persistence.repository.security.BackupCodeRepository;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountDeletionProcessorTest {

    private static final Instant NOW = Instant.parse("2026-08-09T03:23:00Z");
    private static final String USER_ID = "01USER00000000000000000000";

    @Mock private AccountDeletionRequestRepository deletionRequestRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuthSessionRepository authSessionRepository;
    @Mock private SocialAccountRepository socialAccountRepository;
    @Mock private BackupCodeRepository backupCodeRepository;
    @Mock private KycDocumentStoragePort kycDocumentStoragePort;
    @Mock private JdbcTemplate jdbcTemplate;

    private AccountDeletionProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new AccountDeletionProcessor(
                deletionRequestRepository,
                userRepository,
                authSessionRepository,
                socialAccountRepository,
                backupCodeRepository,
                kycDocumentStoragePort,
                jdbcTemplate,
                new AccountManagementProperties(
                        new AccountManagementProperties.Otp(300, 5),
                        new AccountManagementProperties.Deletion(30, 100)),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void tombstonesDueUserAndDeletesAllIdentityOwnedData() {
        UserEntity user = activeUser();
        AccountDeletionRequestEntity request = request(user);
        AuthSessionEntity activeSession = session("active-session", AuthSessionStatus.ACTIVE);
        AuthSessionEntity expiredSession = session("expired-session", AuthSessionStatus.REVOKED);

        when(deletionRequestRepository.findDueForUpdate(eq(AccountDeletionStatus.PENDING), eq(NOW), any()))
                .thenReturn(List.of(request));
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(authSessionRepository.findByUser_UserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(List.of(activeSession, expiredSession));
        when(jdbcTemplate.queryForList(any(String.class), eq(String.class), eq(USER_ID)))
                .thenReturn(List.of("kyc-public-1", "kyc-public-2"));

        AccountDeletionProcessor.Result result = processor.completeDueRequests();

        assertThat(result.completedAccounts()).isOne();
        assertThat(result.revokedSessionIds()).containsExactly("active-session", "expired-session");
        assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
        assertThat(user.getDeletedAt()).isEqualTo(NOW);
        assertThat(user.getEmail()).isNull();
        assertThat(user.getPhone()).isNull();
        assertThat(user.getUsername()).isEqualTo("deleted_" + USER_ID);
        assertThat(user.getPasswordHash()).isNull();
        assertThat(user.isMfaEnabled()).isFalse();
        assertThat(request.getStatus()).isEqualTo(AccountDeletionStatus.COMPLETED);
        assertThat(request.getCompletedAt()).isEqualTo(NOW);

        verify(kycDocumentStoragePort).delete("kyc-public-1");
        verify(kycDocumentStoragePort).delete("kyc-public-2");
        verify(jdbcTemplate).update(
                "DELETE FROM kyc_documents WHERE kyc_id IN (SELECT kyc_id FROM kyc_profiles WHERE user_id = ?)",
                USER_ID);
        verify(jdbcTemplate).update("DELETE FROM kyc_profiles WHERE user_id = ?", USER_ID);
        verify(jdbcTemplate).update("DELETE FROM user_addresses WHERE user_id = ?", USER_ID);
        verify(jdbcTemplate).update("DELETE FROM user_consents WHERE user_id = ?", USER_ID);
        verify(jdbcTemplate).update("DELETE FROM user_preferences WHERE user_id = ?", USER_ID);
        verify(jdbcTemplate).update("DELETE FROM user_feedbacks WHERE user_id = ?", USER_ID);
        verify(jdbcTemplate).update("DELETE FROM data_export_requests WHERE user_id = ?", USER_ID);
        verify(jdbcTemplate).update("DELETE FROM user_roles WHERE user_id = ?", USER_ID);
        verify(jdbcTemplate).update("DELETE FROM agent_identities WHERE owner_id = ?", USER_ID);
        verify(jdbcTemplate).update("DELETE FROM auth_sessions WHERE user_id = ?", USER_ID);
        verify(jdbcTemplate).update(
                "UPDATE security_audits SET ip_address = NULL, device_id = NULL, description = ? WHERE user_id = ?",
                "Anonymized on account deletion", USER_ID);
        verify(socialAccountRepository).deleteByUser_UserId(USER_ID);
        verify(backupCodeRepository).deleteByUser_UserId(USER_ID);
    }

    @Test
    void abortsBeforeDeletingRowsWhenKycAssetDeletionFails() {
        UserEntity user = activeUser();
        AccountDeletionRequestEntity request = request(user);

        when(deletionRequestRepository.findDueForUpdate(eq(AccountDeletionStatus.PENDING), eq(NOW), any()))
                .thenReturn(List.of(request));
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(jdbcTemplate.queryForList(any(String.class), eq(String.class), eq(USER_ID)))
                .thenReturn(List.of("kyc-public-1"));
        doThrow(new IllegalStateException("cloudinary down"))
                .when(kycDocumentStoragePort).delete("kyc-public-1");

        assertThatThrownBy(() -> processor.completeDueRequests())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cloudinary down");

        verify(jdbcTemplate, never()).update(
                org.mockito.ArgumentMatchers.startsWith("DELETE FROM kyc_documents"), eq(USER_ID));
    }

    @Test
    void doesNothingWhenNoDeletionIsDue() {
        when(deletionRequestRepository.findDueForUpdate(eq(AccountDeletionStatus.PENDING), eq(NOW), any()))
                .thenReturn(List.of());

        AccountDeletionProcessor.Result result = processor.completeDueRequests();

        assertThat(result.completedAccounts()).isZero();
        assertThat(result.revokedSessionIds()).isEmpty();
        verify(userRepository, never()).findByIdForUpdate(any());
    }

    private static UserEntity activeUser() {
        return UserEntity.builder()
                .userId(USER_ID)
                .email("buyer@example.com")
                .phone("+84901234567")
                .username("buyer")
                .passwordHash("hash")
                .displayName("Buyer")
                .avatarUrl("avatar")
                .status(UserStatus.ACTIVE)
                .mfaEnabled(true)
                .mfaSecret("secret")
                .failedLoginAttempts(2)
                .build();
    }

    private static AccountDeletionRequestEntity request(UserEntity user) {
        return AccountDeletionRequestEntity.builder()
                .deletionRequestId("01DELETE000000000000000000")
                .user(user)
                .status(AccountDeletionStatus.PENDING)
                .scheduledDeletionAt(NOW.minusSeconds(1))
                .build();
    }

    private static AuthSessionEntity session(String sessionId, AuthSessionStatus status) {
        return AuthSessionEntity.builder()
                .sessionId(sessionId)
                .status(status.name())
                .build();
    }
}
