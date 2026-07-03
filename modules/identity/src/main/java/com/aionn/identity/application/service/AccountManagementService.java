package com.aionn.identity.application.service;

import com.aionn.identity.application.dto.user.command.CancelAccountDeletionCommand;
import com.aionn.identity.application.dto.user.command.RequestAccountDeletionCommand;
import com.aionn.identity.application.dto.user.command.RequestDataExportCommand;
import com.aionn.identity.application.dto.user.view.DataExportRequestView;
import com.aionn.identity.application.dto.user.view.DeletionRequestView;
import com.aionn.identity.application.dto.user.view.UserProfileView;
import com.aionn.identity.application.mapper.UserResultMapper;
import com.aionn.identity.application.policy.AccountManagementPolicy;
import com.aionn.identity.application.port.out.auth.AuthSessionPersistencePort;
import com.aionn.identity.application.port.out.auth.RefreshTokenStorePort;
import com.aionn.identity.application.port.out.integration.IdentityIntegrationEventPublisherPort;
import com.aionn.identity.application.port.out.user.UserOtpChallengeStorePort;
import com.aionn.sharedkernel.integration.port.notification.IdentityNotificationPort;
import com.aionn.identity.domain.valueobject.UserOtpPurpose;
import com.aionn.identity.application.port.out.user.UserPersistencePort;
import com.aionn.identity.application.port.out.user.AccountDeletionPort;
import com.aionn.identity.application.port.out.user.DataExportPort;
import com.aionn.identity.application.port.out.user.UserOtpChallengeStorePort.UserOtpChallenge;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.model.AuthSession;
import com.aionn.identity.domain.model.IdentityUser;
import com.aionn.identity.domain.valueobject.AuthSessionStatus;
import com.aionn.identity.domain.valueobject.OtpChannel;
import com.aionn.sharedkernel.util.OtpGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AccountManagementService {

    private final UserPersistencePort userPersistencePort;
    private final IdentityNotificationPort notificationPort;
    private final IdentityIntegrationEventPublisherPort integrationEventPublisher;
    private final UserOtpChallengeStorePort userOtpChallengeStore;
    private final AccountDeletionPort accountDeletionPort;
    private final DataExportPort dataExportPort;
    private final AuthSessionPersistencePort authSessionPersistencePort;
    private final RefreshTokenStorePort refreshTokenStore;
    private final AccountManagementPolicy accountManagementPolicy;
    private final UserResultMapper userResultMapper;

    public DeletionRequestView requestAccountDeletion(RequestAccountDeletionCommand command) {
        return requestAccountDeletion(command.userId());
    }

    public void cancelAccountDeletion(CancelAccountDeletionCommand command) {
        cancelAccountDeletion(command.userId());
    }

    public DataExportRequestView requestDataExport(RequestDataExportCommand command) {
        return requestDataExport(command.userId());
    }

    public void sendVerifyPrimaryEmailOtp(String userId) {
        IdentityUser user = getActiveUser(userId);
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IdentityException(IdentityErrorCode.EMAIL_REQUIRED, "User has no primary email");
        }
        String otpCode = OtpGenerator.generate6DigitOtp();
        userOtpChallengeStore.save(new UserOtpChallenge(
                userId,
                UserOtpPurpose.VERIFY_PRIMARY_EMAIL,
                OtpChannel.EMAIL,
                user.getEmail(),
                otpCode,
                null,
                nowUtc().plusSeconds(accountManagementPolicy.getOtpExpirySeconds()),
                0));
        notificationPort.sendEmailOtp(user.getEmail(), otpCode);
        log.info("Verification OTP sent to email for user: {}", userId);
    }

    public void confirmVerifyPrimaryEmailOtp(String userId, String otpCode) {
        IdentityUser user = getActiveUser(userId);
        UserOtpChallenge challenge = getChallenge(
                userId,
                UserOtpPurpose.VERIFY_PRIMARY_EMAIL,
                IdentityErrorCode.EMAIL_VERIFICATION_NOT_FOUND);
        validateOtp(challenge, otpCode);
        user.verifyEmail();
        userPersistencePort.save(user);
        userOtpChallengeStore.delete(userId, UserOtpPurpose.VERIFY_PRIMARY_EMAIL);
        log.info("Email verified for user: {}", userId);
    }

    public void requestEmailChangeOtp(String userId, String newEmail) {
        IdentityUser user = getActiveUser(userId);
        if (newEmail == null || newEmail.isBlank()) {
            throw new IdentityException(IdentityErrorCode.EMAIL_REQUIRED, "New email must not be blank");
        }
        userPersistencePort.findByIdentity(newEmail)
                .filter(existing -> !existing.getUserId().equals(user.getUserId()))
                .ifPresent(existing -> {
                    throw new IdentityException(IdentityErrorCode.EMAIL_ALREADY_EXISTS);
                });

        String otpCode = OtpGenerator.generate6DigitOtp();
        userOtpChallengeStore.save(new UserOtpChallenge(
                userId,
                UserOtpPurpose.CHANGE_EMAIL,
                OtpChannel.EMAIL,
                newEmail,
                otpCode,
                newEmail,
                nowUtc().plusSeconds(accountManagementPolicy.getOtpExpirySeconds()),
                0));
        notificationPort.sendEmailOtp(newEmail, otpCode);
        log.info("Email change OTP sent to new email for user: {}", userId);
    }

    public UserProfileView confirmEmailChange(String userId, String otpCode) {
        IdentityUser user = getActiveUser(userId);
        UserOtpChallenge challenge = getChallenge(userId, UserOtpPurpose.CHANGE_EMAIL,
                IdentityErrorCode.EMAIL_CHANGE_NOT_FOUND);
        validateOtp(challenge, otpCode);

        userPersistencePort.findByIdentity(challenge.pendingValue())
                .filter(existing -> !existing.getUserId().equals(user.getUserId()))
                .ifPresent(existing -> {
                    throw new IdentityException(IdentityErrorCode.EMAIL_ALREADY_EXISTS);
                });

        String oldEmail = user.getEmail();
        user.updateEmail(challenge.pendingValue());
        IdentityUser saved = userPersistencePort.save(user);
        userOtpChallengeStore.delete(userId, UserOtpPurpose.CHANGE_EMAIL);

        revokeAllSessions(userId);
        integrationEventPublisher.publishEmailChanged(userId, oldEmail, saved.getEmail());
        log.info("Email changed for user: {}", userId);
        return userResultMapper.toUserProfileView(saved);
    }

    public void requestPhoneChangeOtp(String userId, String newPhone) {
        getActiveUser(userId);
        if (newPhone == null || newPhone.isBlank()) {
            throw new IdentityException(IdentityErrorCode.PHONE_REQUIRED, "New phone must not be blank");
        }
        if (userPersistencePort.existsByPhone(newPhone)) {
            throw new IdentityException(IdentityErrorCode.PHONE_ALREADY_EXISTS);
        }

        String otpCode = OtpGenerator.generate6DigitOtp();
        userOtpChallengeStore.save(new UserOtpChallenge(
                userId,
                UserOtpPurpose.CHANGE_PHONE,
                OtpChannel.PHONE,
                newPhone,
                otpCode,
                newPhone,
                nowUtc().plusSeconds(accountManagementPolicy.getOtpExpirySeconds()),
                0));
        notificationPort.sendPhoneOtp(newPhone, otpCode);
        log.info("Phone change OTP sent to new phone for user: {}", userId);
    }

    public UserProfileView confirmPhoneChange(String userId, String otpCode) {
        IdentityUser user = getActiveUser(userId);
        UserOtpChallenge challenge = getChallenge(userId, UserOtpPurpose.CHANGE_PHONE,
                IdentityErrorCode.PHONE_CHANGE_NOT_FOUND);
        validateOtp(challenge, otpCode);

        if (userPersistencePort.existsByPhone(challenge.pendingValue())
                && !challenge.pendingValue().equals(user.getPhone())) {
            throw new IdentityException(IdentityErrorCode.PHONE_ALREADY_EXISTS);
        }

        String oldPhone = user.getPhone();
        user.updatePhone(challenge.pendingValue());
        IdentityUser saved = userPersistencePort.save(user);
        userOtpChallengeStore.delete(userId, UserOtpPurpose.CHANGE_PHONE);

        revokeAllSessions(userId);
        integrationEventPublisher.publishPhoneChanged(userId, oldPhone, saved.getPhone());
        log.info("Phone changed for user: {}", userId);
        return userResultMapper.toUserProfileView(saved);
    }

    private DeletionRequestView requestAccountDeletion(String userId) {
        getActiveUser(userId);
        if (accountDeletionPort.findPendingByUserId(userId).isPresent()) {
            throw new IdentityException(IdentityErrorCode.ACCOUNT_DELETION_ALREADY_REQUESTED);
        }
        LocalDateTime scheduledDeletionAt = nowUtc()
                .plusDays(accountManagementPolicy.getDeletionGraceDays());
        DeletionRequestView result = accountDeletionPort.save(userId, scheduledDeletionAt);
        log.info("Account deletion requested for user: {}, scheduled at: {}", userId, scheduledDeletionAt);
        return result;
    }

    private void cancelAccountDeletion(String userId) {
        getActiveUser(userId);
        if (accountDeletionPort.findPendingByUserId(userId).isEmpty()) {
            throw new IdentityException(IdentityErrorCode.ACCOUNT_DELETION_NOT_FOUND);
        }
        accountDeletionPort.cancel(userId);
        log.info("Account deletion canceled for user: {}", userId);
    }

    private DataExportRequestView requestDataExport(String userId) {
        getActiveUser(userId);
        if (dataExportPort.hasActiveRequest(userId)) {
            throw new IdentityException(IdentityErrorCode.DATA_EXPORT_ALREADY_IN_PROGRESS);
        }
        DataExportRequestView result = dataExportPort.save(userId);
        log.info("Data export requested for user: {}", userId);
        return result;
    }

    private void revokeAllSessions(String userId) {
        var sessions = authSessionPersistencePort.findByUserId(userId);
        for (AuthSession session : sessions) {
            if (AuthSessionStatus.ACTIVE.equals(session.getStatus())) {
                session.revoke();
                refreshTokenStore.revokeBySessionId(session.getSessionId());
            }
        }
        authSessionPersistencePort.saveAll(sessions);
    }

    private UserOtpChallenge getChallenge(String userId, UserOtpPurpose purpose, IdentityErrorCode notFoundCode) {
        return userOtpChallengeStore.find(userId, purpose)
                .orElseThrow(() -> new IdentityException(notFoundCode));
    }

    private void validateOtp(UserOtpChallenge challenge, String otpCode) {
        if (otpCode == null || otpCode.isBlank()) {
            throw new IdentityException(IdentityErrorCode.OTP_REQUIRED);
        }
        if (challenge.expiresAt().isBefore(nowUtc())) {
            userOtpChallengeStore.delete(challenge.userId(), challenge.purpose());
            throw new IdentityException(IdentityErrorCode.OTP_EXPIRED);
        }
        if (!challenge.otpCode().equals(otpCode)) {
            int attempts = challenge.attempts() + 1;
            if (attempts >= accountManagementPolicy.getOtpMaxAttempts()) {
                userOtpChallengeStore.delete(challenge.userId(), challenge.purpose());
                throw new IdentityException(IdentityErrorCode.OTP_ATTEMPTS_EXCEEDED);
            }
            userOtpChallengeStore.save(challenge.withAttempts(attempts));
            throw new IdentityException(IdentityErrorCode.OTP_INVALID);
        }
    }

    private IdentityUser getActiveUser(String userId) {
        IdentityUser user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.USER_NOT_FOUND));
        if (!user.isActive()) {
            throw new IdentityException(IdentityErrorCode.USER_INACTIVE);
        }
        return user;
    }

    private static LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
