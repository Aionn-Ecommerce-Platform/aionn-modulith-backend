package com.aionn.identity.infrastructure.scheduling;

import com.aionn.identity.domain.valueobject.AccountDeletionStatus;
import com.aionn.identity.domain.valueobject.AgentStatus;
import com.aionn.identity.domain.valueobject.AuthSessionStatus;
import com.aionn.identity.domain.valueobject.UserStatus;
import com.aionn.identity.infrastructure.persistence.entity.AuthSessionEntity;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import com.aionn.identity.infrastructure.persistence.repository.account.AccountDeletionRequestRepository;
import com.aionn.identity.infrastructure.persistence.repository.agent.AgentIdentityRepository;
import com.aionn.identity.infrastructure.persistence.repository.auth.AuthSessionRepository;
import com.aionn.identity.infrastructure.persistence.repository.auth.SocialAccountRepository;
import com.aionn.identity.infrastructure.persistence.repository.security.BackupCodeRepository;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AccountDeletionProcessor {

    static final int BATCH_SIZE = 100;
    private static final String DELETED_USERNAME_PREFIX = "deleted_";

    private final AccountDeletionRequestRepository deletionRequestRepository;
    private final UserRepository userRepository;
    private final AuthSessionRepository authSessionRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final BackupCodeRepository backupCodeRepository;
    private final AgentIdentityRepository agentIdentityRepository;
    private final Clock clock;

    @Transactional
    public Result completeDueRequests() {
        Instant now = clock.instant();
        var requests = deletionRequestRepository.findDueForUpdate(
                AccountDeletionStatus.PENDING, now, PageRequest.of(0, BATCH_SIZE));
        List<String> revokedSessionIds = new ArrayList<>();
        int completed = 0;

        for (var request : requests) {
            UserEntity user = userRepository.findByIdForUpdate(request.getUser().getUserId()).orElse(null);
            if (user == null) {
                continue;
            }
            if (!UserStatus.DELETED.equals(user.getStatus())) {
                tombstone(user, now);
            }
            socialAccountRepository.deleteByUser_UserId(user.getUserId());
            backupCodeRepository.deleteByUser_UserId(user.getUserId());
            agentIdentityRepository.findByOwner_UserId(user.getUserId())
                    .forEach(agent -> agent.setStatus(AgentStatus.REVOKED.name()));
            revokeSessions(user.getUserId(), revokedSessionIds);
            request.setStatus(AccountDeletionStatus.COMPLETED);
            request.setCompletedAt(now);
            completed++;
        }
        return new Result(completed, List.copyOf(revokedSessionIds));
    }

    private static void tombstone(UserEntity user, Instant now) {
        user.setEmail(null);
        user.setPhone(null);
        user.setUsername(DELETED_USERNAME_PREFIX + user.getUserId());
        user.setPasswordHash(null);
        user.setDisplayName("Deleted user");
        user.setAvatarUrl(null);
        user.setEmailVerifiedAt(null);
        user.setPhoneVerifiedAt(null);
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setStatus(UserStatus.DELETED);
        user.setDeletedAt(now);
    }

    private void revokeSessions(String userId, List<String> revokedSessionIds) {
        for (AuthSessionEntity session : authSessionRepository.findByUser_UserIdOrderByCreatedAtDesc(userId)) {
            revokedSessionIds.add(session.getSessionId());
            if (AuthSessionStatus.ACTIVE.name().equals(session.getStatus())) {
                session.setStatus(AuthSessionStatus.REVOKED.name());
            }
        }
    }

    public record Result(int completedAccounts, List<String> revokedSessionIds) {
    }
}
