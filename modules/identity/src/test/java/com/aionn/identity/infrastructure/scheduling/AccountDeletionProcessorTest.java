package com.aionn.identity.infrastructure.scheduling;

import com.aionn.identity.domain.valueobject.AccountDeletionStatus;
import com.aionn.identity.domain.valueobject.AgentStatus;
import com.aionn.identity.domain.valueobject.AuthSessionStatus;
import com.aionn.identity.domain.valueobject.UserStatus;
import com.aionn.identity.infrastructure.persistence.entity.AccountDeletionRequestEntity;
import com.aionn.identity.infrastructure.persistence.entity.AgentIdentityEntity;
import com.aionn.identity.infrastructure.persistence.entity.AuthSessionEntity;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import com.aionn.identity.infrastructure.persistence.repository.account.AccountDeletionRequestRepository;
import com.aionn.identity.infrastructure.persistence.repository.agent.AgentIdentityRepository;
import com.aionn.identity.infrastructure.persistence.repository.auth.AuthSessionRepository;
import com.aionn.identity.infrastructure.persistence.repository.auth.SocialAccountRepository;
import com.aionn.identity.infrastructure.persistence.repository.security.BackupCodeRepository;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountDeletionProcessorTest {

    private static final Instant NOW = Instant.parse("2026-08-09T03:23:00Z");

    @Mock private AccountDeletionRequestRepository deletionRequestRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuthSessionRepository authSessionRepository;
    @Mock private SocialAccountRepository socialAccountRepository;
    @Mock private BackupCodeRepository backupCodeRepository;
    @Mock private AgentIdentityRepository agentIdentityRepository;

    private AccountDeletionProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new AccountDeletionProcessor(
                deletionRequestRepository,
                userRepository,
                authSessionRepository,
                socialAccountRepository,
                backupCodeRepository,
                agentIdentityRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void tombstonesDueUserAndRevokesAuthenticationMaterial() {
        UserEntity user = UserEntity.builder()
                .userId("01USER00000000000000000000")
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
        AccountDeletionRequestEntity request = AccountDeletionRequestEntity.builder()
                .deletionRequestId("01DELETE000000000000000000")
                .user(user)
                .status(AccountDeletionStatus.PENDING)
                .scheduledDeletionAt(NOW.minusSeconds(1))
                .build();
        AuthSessionEntity activeSession = AuthSessionEntity.builder()
                .sessionId("active-session")
                .status(AuthSessionStatus.ACTIVE.name())
                .build();
        AuthSessionEntity expiredSession = AuthSessionEntity.builder()
                .sessionId("expired-session")
                .status(AuthSessionStatus.REVOKED.name())
                .build();
        AgentIdentityEntity agent = AgentIdentityEntity.builder()
                .agentId("agent-1")
                .status(AgentStatus.ACTIVE.name())
                .build();

        when(deletionRequestRepository.findDueForUpdate(eq(AccountDeletionStatus.PENDING), eq(NOW), any()))
                .thenReturn(List.of(request));
        when(userRepository.findByIdForUpdate(user.getUserId())).thenReturn(Optional.of(user));
        when(authSessionRepository.findByUser_UserIdOrderByCreatedAtDesc(user.getUserId()))
                .thenReturn(List.of(activeSession, expiredSession));
        when(agentIdentityRepository.findByOwner_UserId(user.getUserId())).thenReturn(List.of(agent));

        AccountDeletionProcessor.Result result = processor.completeDueRequests();

        assertThat(result.completedAccounts()).isOne();
        assertThat(result.revokedSessionIds()).containsExactly("active-session", "expired-session");
        assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
        assertThat(user.getDeletedAt()).isEqualTo(NOW);
        assertThat(user.getEmail()).isNull();
        assertThat(user.getPhone()).isNull();
        assertThat(user.getUsername()).isEqualTo("deleted_" + user.getUserId());
        assertThat(user.getPasswordHash()).isNull();
        assertThat(user.isMfaEnabled()).isFalse();
        assertThat(activeSession.getStatus()).isEqualTo(AuthSessionStatus.REVOKED.name());
        assertThat(agent.getStatus()).isEqualTo(AgentStatus.REVOKED.name());
        assertThat(request.getStatus()).isEqualTo(AccountDeletionStatus.COMPLETED);
        assertThat(request.getCompletedAt()).isEqualTo(NOW);
        verify(socialAccountRepository).deleteByUser_UserId(user.getUserId());
        verify(backupCodeRepository).deleteByUser_UserId(user.getUserId());
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
}
