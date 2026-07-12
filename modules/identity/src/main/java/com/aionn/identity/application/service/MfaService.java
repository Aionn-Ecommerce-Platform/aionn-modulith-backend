package com.aionn.identity.application.service;

import com.aionn.identity.application.dto.security.result.MfaResult;
import com.aionn.identity.application.dto.security.result.MfaSetupResult;
import com.aionn.identity.application.policy.MfaPolicy;
import com.aionn.identity.application.port.out.security.MfaPersistencePort;
import com.aionn.identity.application.port.out.security.PasswordHasherPort;
import com.aionn.identity.application.port.out.security.SecurityAuditPort;
import com.aionn.identity.application.port.out.security.TotpManagerPort;
import com.aionn.identity.application.port.out.observability.IdentityMetricsPort;
import com.aionn.identity.domain.valueobject.SecurityAuditEventType;
import com.aionn.identity.application.port.out.security.UserSecurityPort;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.sharedkernel.util.OtpGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Clock;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MfaService {

    private final UserSecurityPort userSecurityPort;
    private final MfaPersistencePort mfaPersistencePort;
    private final SecurityAuditPort securityAuditPort;
    private final PasswordHasherPort passwordHasher;
    private final TotpManagerPort totpManager;
    private final MfaPolicy mfaPolicy;
    private final IdentityMetricsPort identityMetrics;

    private final Clock clock;
    public MfaSetupResult initiateSetup(
            String userId,
            String password,
            String ipAddress) {
        log.info("Initiating MFA setup for user: {}", userId);
        var user = getUserSecurityData(userId);
        verifyPassword(user, password);
        if (user.mfaEnabled()) {
            throw new IdentityException(IdentityErrorCode.MFA_ALREADY_ENABLED);
        }

        String secret = totpManager.generateSecret();
        mfaPersistencePort.saveMfaSecret(userId, secret);
        mfaPersistencePort.updateMfaStatus(userId, false);
        mfaPersistencePort.deleteBackupCodes(userId);
        securityAuditPort.saveAuditLog(userId, SecurityAuditEventType.MFA_SETUP_INITIATED, ipAddress);
        return new MfaSetupResult(
                secret,
                totpManager.buildOtpAuthUri(mfaPolicy.getMfaIssuer(), userId, secret),
                mfaPolicy.getMfaIssuer(),
                userId);
    }

    public MfaResult enableMfa(
            String userId,
            String password,
            String mfaCode,
            String ipAddress) {
        log.info("Enabling MFA for user: {}", userId);
        var user = getUserSecurityData(userId);
        verifyPassword(user, password);
        verifyTotpChallenge(user, mfaCode);
        mfaPersistencePort.updateMfaStatus(userId, true);
        List<String> backupCodes = replaceBackupCodes(userId);
        securityAuditPort.saveAuditLog(userId, SecurityAuditEventType.MFA_ENABLED, ipAddress);
        identityMetrics.mfaVerification("success");
        return new MfaResult(true, backupCodes);
    }

    public MfaResult disableMfa(
            String userId,
            String password,
            String mfaCode,
            String ipAddress) {
        log.info("Disabling MFA for user: {}", userId);
        var user = getUserSecurityData(userId);
        verifyPassword(user, password);
        verifySecondFactor(user, mfaCode);
        mfaPersistencePort.clearMfa(userId);
        securityAuditPort.saveAuditLog(userId, SecurityAuditEventType.MFA_DISABLED, ipAddress);
        return new MfaResult(false, null);
    }

    public List<String> regenerateBackupCodes(String userId, String password, String mfaCode, String ipAddress) {
        log.info("Regenerating backup codes for user: {}", userId);
        var user = getUserSecurityData(userId);
        verifyPassword(user, password);
        verifySecondFactor(user, mfaCode);
        List<String> rawCodes = replaceBackupCodes(userId);

        securityAuditPort.saveAuditLog(userId, SecurityAuditEventType.MFA_BACKUP_CODES_REGENERATED, ipAddress);
        return rawCodes;
    }

    private void verifyPassword(UserSecurityPort.UserSecurityData user, String password) {
        if (password == null || password.isBlank()
                || user.passwordHash() == null
                || !passwordHasher.matches(password, user.passwordHash())) {
            log.warn("MFA operation rejected for user {} due to bad password", user.userId());
            throw new IdentityException(IdentityErrorCode.INVALID_CREDENTIALS);
        }
    }

    private UserSecurityPort.UserSecurityData getUserSecurityData(String userId) {
        return userSecurityPort.findById(userId)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.USER_NOT_FOUND));
    }

    private void verifyTotpChallenge(UserSecurityPort.UserSecurityData user, String mfaCode) {
        if (user.mfaSecret() == null || user.mfaSecret().isBlank()) {
            throw new IdentityException(IdentityErrorCode.MFA_SETUP_NOT_INITIATED);
        }
        if (!totpManager.verifyCode(user.mfaSecret(), mfaCode)) {
            throw new IdentityException(IdentityErrorCode.OTP_INVALID, "Invalid MFA code");
        }
    }

    private void verifySecondFactor(UserSecurityPort.UserSecurityData user, String mfaCode) {
        if (!user.mfaEnabled() || user.mfaSecret() == null || user.mfaSecret().isBlank()) {
            throw new IdentityException(IdentityErrorCode.MFA_NOT_ENABLED);
        }
        if (totpManager.verifyCode(user.mfaSecret(), mfaCode)) {
            return;
        }
        boolean matched = mfaPersistencePort.findActiveBackupCodes(user.userId()).stream()
                .filter(code -> passwordHasher.matches(mfaCode, code.codeHash()))
                .findFirst()
                .map(code -> mfaPersistencePort.markBackupCodeUsed(code.backupCodeId(), nowUtc()))
                .orElse(false);
        if (!matched) {
            throw new IdentityException(IdentityErrorCode.OTP_INVALID, "Invalid MFA code");
        }
    }

    private List<String> replaceBackupCodes(String userId) {
        mfaPersistencePort.deleteBackupCodes(userId);
        List<String> rawCodes = IntStream.range(0, mfaPolicy.getBackupCodeCount())
                .mapToObj(i -> OtpGenerator.generate8DigitOtp())
                .toList();
        List<String> codeHashes = rawCodes.stream()
                .map(passwordHasher::hash)
                .toList();
        mfaPersistencePort.saveBackupCodes(userId, codeHashes);
        return rawCodes;
    }
    private Instant nowUtc() {
        return clock.instant();
    }
}
