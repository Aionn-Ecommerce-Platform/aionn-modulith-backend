package com.aionn.identity.application.service;

import com.aionn.identity.application.policy.AuthPolicy;
import com.aionn.identity.application.policy.IdentityValidationConstants;
import com.aionn.identity.application.port.out.auth.AuthSessionPersistencePort;
import com.aionn.identity.application.port.out.auth.RefreshTokenStorePort;
import com.aionn.identity.application.port.out.integration.IdentityIntegrationEventPublisherPort;
import com.aionn.identity.application.port.out.observability.IdentityMetricsPort;
import com.aionn.sharedkernel.integration.port.notification.IdentityNotificationPort;
import com.aionn.identity.application.port.out.security.PasswordHasherPort;
import com.aionn.identity.application.port.out.security.PasswordResetPort;
import com.aionn.identity.application.port.out.security.SecurityAuditPort;
import com.aionn.identity.application.port.out.security.UserSecurityPort;
import com.aionn.identity.domain.valueobject.SecurityAuditEventType;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.model.AuthSession;
import com.aionn.identity.domain.valueobject.AuthSessionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserSecurityPort userSecurityPort;
    private final PasswordResetPort passwordResetPort;
    private final SecurityAuditPort securityAuditPort;
    private final PasswordHasherPort passwordHasher;
    private final AuthSessionPersistencePort authSessionPersistencePort;
    private final RefreshTokenStorePort refreshTokenStore;
    private final IdentityNotificationPort notificationPort;
    private final IdentityIntegrationEventPublisherPort integrationEventPublisher;
    private final IdentityMetricsPort identityMetrics;
    private final AuthPolicy authPolicy;

    public void changePassword(String userId, String currentPassword, String newPassword, String ipAddress) {
        log.info("Processing password change for user: {}", userId);
        validateNewPassword(newPassword);
        if (currentPassword != null && currentPassword.equals(newPassword)) {
            throw new IdentityException(IdentityErrorCode.OTP_INVALID,
                    "New password must differ from the current password");
        }

        var user = getUserSecurityData(userId);
        if (!passwordHasher.matches(currentPassword, user.passwordHash())) {
            log.warn("Failed password change for user {} - invalid current password", userId);
            throw new IdentityException(IdentityErrorCode.INVALID_CREDENTIALS);
        }

        passwordResetPort.updatePassword(userId, passwordHasher.hash(newPassword));
        revokeOtherSessions(userId);
        securityAuditPort.saveAuditLog(userId, SecurityAuditEventType.PASSWORD_CHANGED, ipAddress);
        integrationEventPublisher.publishPasswordChanged(userId, "self-service");
    }

    public void requestPasswordReset(String identity, String ipAddress) {
        log.info("Processing password reset request");
        var user = userSecurityPort.findByIdentity(identity);
        if (user.isEmpty()) {
            log.debug("Password reset requested for unknown identity (silently ignored)");
            return;
        }

        byte[] bytes = new byte[IdentityValidationConstants.RESET_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        passwordResetPort.savePasswordResetToken(token, user.get().userId(),
                LocalDateTime.now().plusMinutes(authPolicy.getPasswordResetTokenTtlMinutes()));
        securityAuditPort.saveAuditLog(user.get().userId(), SecurityAuditEventType.PASSWORD_RESET_REQUESTED, ipAddress);
        identityMetrics.passwordResetLifecycle("requested");
        try {
            notificationPort.sendPasswordResetRequested(user.get().userId(), token);
        } catch (RuntimeException ex) {
            log.error("Failed to dispatch password reset token for user {}", user.get().userId(), ex);
        }
    }

    public void completePasswordReset(String token, String newPassword, String ipAddress) {
        log.info("Processing password reset completion");
        validateNewPassword(newPassword);

        var data = passwordResetPort.consumePasswordResetToken(token)
                .orElseThrow(() -> {
                    log.warn("Invalid password reset token presented");
                    return new IdentityException(IdentityErrorCode.VERIFICATION_TOKEN_INVALID);
                });
        if (data.expiresAt().isBefore(LocalDateTime.now())) {
            throw new IdentityException(IdentityErrorCode.OTP_EXPIRED);
        }

        passwordResetPort.updatePassword(data.userId(), passwordHasher.hash(newPassword));
        revokeOtherSessions(data.userId());
        securityAuditPort.saveAuditLog(data.userId(), SecurityAuditEventType.PASSWORD_RESET_COMPLETED, ipAddress);
        identityMetrics.passwordResetLifecycle("completed");
        integrationEventPublisher.publishPasswordChanged(data.userId(), "password reset");
    }

    private void revokeOtherSessions(String userId) {
        var sessions = authSessionPersistencePort.findByUserId(userId);
        for (AuthSession session : sessions) {
            if (AuthSessionStatus.ACTIVE.equals(session.getStatus())) {
                session.revoke();
                refreshTokenStore.revokeBySessionId(session.getSessionId());
            }
        }
        authSessionPersistencePort.saveAll(sessions);
    }

    private void validateNewPassword(String newPassword) {
        if (newPassword == null
                || newPassword.length() < IdentityValidationConstants.PASSWORD_MIN_LENGTH
                || newPassword.length() > IdentityValidationConstants.PASSWORD_MAX_LENGTH) {
            throw new IdentityException(IdentityErrorCode.INVALID_CREDENTIALS,
                    "Password must be between " + IdentityValidationConstants.PASSWORD_MIN_LENGTH + " and "
                            + IdentityValidationConstants.PASSWORD_MAX_LENGTH + " characters");
        }
        boolean hasLetter = newPassword.chars().anyMatch(Character::isLetter);
        boolean hasDigit = newPassword.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new IdentityException(IdentityErrorCode.INVALID_CREDENTIALS,
                    "Password must contain at least one letter and one digit");
        }
    }

    private UserSecurityPort.UserSecurityData getUserSecurityData(String userId) {
        return userSecurityPort.findById(userId)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.USER_NOT_FOUND));
    }
}
