package com.aionn.identity.infrastructure.scheduling;

import com.aionn.identity.domain.valueobject.AccountDeletionStatus;
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
import com.aionn.identity.application.port.out.kyc.KycDocumentStoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Executes one transactional batch of account-deletion requests whose grace period has
 * expired. The scheduler calls this repeatedly until a batch comes back short.
 */
@Component
@RequiredArgsConstructor
public class AccountDeletionProcessor {

    private static final String DELETED_USERNAME_PREFIX = "deleted_";
    private static final String ANONYMIZED_AUDIT_DESCRIPTION = "Anonymized on account deletion";
    private final AccountDeletionRequestRepository deletionRequestRepository;
    private final UserRepository userRepository;
    private final AuthSessionRepository authSessionRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final BackupCodeRepository backupCodeRepository;
    private final KycDocumentStoragePort kycDocumentStoragePort;
    private final JdbcTemplate jdbcTemplate;
    private final AccountManagementProperties properties;
    private final Clock clock;

    /** Shared with the scheduler to detect when the final, short batch has been reached. */
    public int batchSize() {
        return properties.deletion().batchSize();
    }

    @Transactional
    public Result completeDueRequests() {
        Instant now = clock.instant();
        var requests = deletionRequestRepository.findDueForUpdate(
                AccountDeletionStatus.PENDING, now, PageRequest.of(0, batchSize()));
        List<String> revokedSessionIds = new ArrayList<>();
        List<String> deletedUserIds = new ArrayList<>();
        int completed = 0;

        for (AccountDeletionRequestEntity request : requests) {
            UserEntity user = userRepository.findByIdForUpdate(request.getUser().getUserId()).orElse(null);
            if (user == null) {
                continue;
            }
            String userId = user.getUserId();
            if (!UserStatus.DELETED.equals(user.getStatus())) {
                tombstone(user, now);
            }

            // Delete external blobs first. If the provider call fails, the transaction rolls
            // back with the public IDs still present so the next run can retry.
            List<String> kycPublicIds = jdbcTemplate.queryForList(
                    "SELECT public_id FROM kyc_documents WHERE kyc_id IN "
                            + "(SELECT kyc_id FROM kyc_profiles WHERE user_id = ?) "
                            + "AND public_id IS NOT NULL",
                    String.class, userId);
            for (String publicId : kycPublicIds) {
                kycDocumentStoragePort.delete(publicId);
            }

            jdbcTemplate.update("DELETE FROM kyc_documents WHERE kyc_id IN "
                    + "(SELECT kyc_id FROM kyc_profiles WHERE user_id = ?)", userId);
            jdbcTemplate.update("DELETE FROM kyc_profiles WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM user_addresses WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM user_consents WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM user_preferences WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM user_feedbacks WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM data_export_requests WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM agent_identities WHERE owner_id = ?", userId);

            revokeSessions(userId, revokedSessionIds);
            jdbcTemplate.update("DELETE FROM auth_sessions WHERE user_id = ?", userId);

            jdbcTemplate.update("UPDATE security_audits SET ip_address = NULL, device_id = NULL, "
                    + "description = ? WHERE user_id = ?", ANONYMIZED_AUDIT_DESCRIPTION, userId);

            socialAccountRepository.deleteByUser_UserId(userId);
            backupCodeRepository.deleteByUser_UserId(userId);

            request.setStatus(AccountDeletionStatus.COMPLETED);
            request.setCompletedAt(now);
            deletedUserIds.add(userId);
            completed++;
        }
        return new Result(completed, List.copyOf(revokedSessionIds), List.copyOf(deletedUserIds));
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
        }
    }

    public record Result(
            int completedAccounts, List<String> revokedSessionIds, List<String> deletedUserIds) {
    }
}
