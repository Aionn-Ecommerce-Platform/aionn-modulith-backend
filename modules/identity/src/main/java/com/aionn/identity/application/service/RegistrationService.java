package com.aionn.identity.application.service;

import com.aionn.identity.application.dto.registration.command.CompleteRegistrationCommand;
import com.aionn.identity.application.dto.registration.command.InitiateRegistrationCommand;
import com.aionn.identity.application.dto.registration.command.ResendRegistrationOtpCommand;
import com.aionn.identity.application.dto.registration.command.VerifyRegistrationOtpCommand;
import com.aionn.identity.application.dto.registration.result.CompleteRegistrationResult;
import com.aionn.identity.application.dto.registration.result.InitiateRegistrationResult;
import com.aionn.identity.application.dto.registration.result.ResendRegistrationOtpResult;
import com.aionn.identity.application.dto.registration.result.VerifyRegistrationOtpResult;
import com.aionn.identity.application.mapper.RegistrationResultMapper;
import com.aionn.identity.application.policy.RegistrationPolicy;
import com.aionn.identity.application.port.out.auth.AccessTokenIssuerPort;
import com.aionn.identity.application.port.out.auth.AuthSessionPersistencePort;
import com.aionn.identity.application.port.out.auth.RefreshTokenStorePort;
import com.aionn.identity.application.port.out.observability.IdentityMetricsPort;
import com.aionn.sharedkernel.integration.port.notification.IdentityNotificationPort;
import com.aionn.identity.application.port.out.registration.CaptchaTokenValidatorPort;
import com.aionn.identity.application.port.out.registration.RegistrationLockManagerPort;
import com.aionn.identity.application.port.out.registration.RegistrationRateLimiterPort;
import com.aionn.identity.application.port.out.registration.RegistrationSessionStorePort;
import com.aionn.identity.application.port.out.security.PasswordHasherPort;
import com.aionn.identity.application.port.out.user.UserPersistencePort;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.model.AuthSession;
import com.aionn.identity.domain.model.IdentityUser;
import com.aionn.identity.domain.model.RegistrationVerificationSession;
import com.aionn.identity.domain.valueobject.RegistrationOtp;
import com.aionn.sharedkernel.domain.vo.PhoneNumber;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RegistrationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserPersistencePort userPersistencePort;
    private final AuthSessionPersistencePort authSessionPersistencePort;
    private final IdentityNotificationPort notificationPort;
    private final CaptchaTokenValidatorPort captchaTokenValidator;
    private final RegistrationRateLimiterPort registrationRateLimiter;
    private final RegistrationSessionStorePort registrationSessionStore;
    private final AccessTokenIssuerPort accessTokenIssuer;
    private final RefreshTokenStorePort refreshTokenStore;
    private final PasswordHasherPort passwordHasher;
    private final RegistrationResultMapper registrationResultMapper;
    private final RegistrationPolicy registrationPolicy;
    private final RegistrationLockManagerPort registrationLockManager;
    private final IdentityMetricsPort identityMetrics;

    public InitiateRegistrationResult initiate(InitiateRegistrationCommand command) {
        log.debug("Initiating registration for identity: {}", command.identity());

        if (!captchaTokenValidator.isValid(command.captchaToken())) {
            log.warn("Invalid captcha token for registration from IP: {}", command.ipAddress());
            throw new IdentityException(IdentityErrorCode.CAPTCHA_INVALID);
        }

        PhoneNumber phoneNumber = validateAndNormalizePhoneNumber(command.identity());
        if (userPersistencePort.existsByPhone(phoneNumber.value())) {
            throw new IdentityException(IdentityErrorCode.PHONE_ALREADY_EXISTS);
        }
        enforceRateLimits(command.ipAddress(), phoneNumber.value());

        String regId = IdGenerator.ulid();
        RegistrationOtp otp = RegistrationOtp.generate(
                registrationPolicy.getResendCooldownSeconds(),
                registrationPolicy.getOtpExpirySeconds());

        var session = new RegistrationVerificationSession(
                regId,
                phoneNumber.value(),
                otp.getCode(),
                0,
                registrationPolicy.getMaxVerifyAttempts(),
                otp.getResendAvailableAt(),
                otp.getExpiredAt(),
                false,
                null,
                null);

        registrationSessionStore.save(session);
        notificationPort.sendRegistrationOtp(phoneNumber.value(), otp.getCode());

        String responseOtpCode = registrationPolicy.isExposeOtpInResponse() ? otp.getCode() : null;
        identityMetrics.registrationLifecycle("initiated");
        return registrationResultMapper.toInitiateResult(session, responseOtpCode);
    }

    public VerifyRegistrationOtpResult verifyOtp(VerifyRegistrationOtpCommand command) {
        var session = registrationSessionStore.findByRegId(command.regId())
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.REGISTRATION_NOT_FOUND));

        if (session.isVerified()) {
            return registrationResultMapper.toVerifyOtpResult(session.getRegId(), session.getVerificationToken());
        }

        try {
            session.verify(command.otpCode());
        } finally {
            registrationSessionStore.save(session);
        }
        identityMetrics.registrationLifecycle("otp_verified");
        return registrationResultMapper.toVerifyOtpResult(session.getRegId(), session.getVerificationToken());
    }

    public ResendRegistrationOtpResult resendOtp(ResendRegistrationOtpCommand command) {
        var session = registrationSessionStore.findByRegId(command.regId())
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.REGISTRATION_SESSION_NOT_FOUND));

        RegistrationOtp newOtp = RegistrationOtp.generate(
                registrationPolicy.getResendCooldownSeconds(),
                registrationPolicy.getOtpExpirySeconds());

        session.resend(newOtp.getCode(), newOtp.getResendAvailableAt(), newOtp.getExpiredAt());
        registrationSessionStore.save(session);
        notificationPort.sendRegistrationOtp(session.getPhoneNumber(), newOtp.getCode());

        String responseOtpCode = registrationPolicy.isExposeOtpInResponse() ? newOtp.getCode() : null;
        return registrationResultMapper.toResendOtpResult(session, responseOtpCode);
    }

    public CompleteRegistrationResult complete(CompleteRegistrationCommand command) {
        var session = registrationSessionStore.findByRegId(command.regId())
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.REGISTRATION_NOT_FOUND));

        String phoneNumber = session.getPhoneNumber();
        String lockToken = registrationLockManager.tryLock(
                phoneNumber,
                registrationPolicy.getLockTimeoutSeconds());
        if (lockToken == null || lockToken.isEmpty()) {
            throw new IdentityException(IdentityErrorCode.REGISTRATION_IN_PROGRESS);
        }
        registrationLockManager.unlockAfterCompletion(phoneNumber, lockToken);
        if (session.isExpired()) {
            throw new IdentityException(IdentityErrorCode.REGISTRATION_EXPIRED);
        }
        if (!session.isVerified() || !command.verificationToken().equals(session.getVerificationToken())) {
            throw new IdentityException(IdentityErrorCode.VERIFICATION_TOKEN_INVALID);
        }

        validateUniqueIdentity(phoneNumber, command.username());

        IdentityUser user = createUser(phoneNumber, command.username(), command.password());
        IdentityUser savedUser = userPersistencePort.save(user);

        AuthSession authSession = createAuthSession(
                savedUser.getUserId(),
                command.ipAddress(),
                command.userAgent());
        AuthSession savedSession = authSessionPersistencePort.save(authSession);

        String accessToken = accessTokenIssuer.issueAccessToken(
                savedUser.getUserId(),
                savedSession.getSessionId(),
                savedSession.getExpiresAt(),
                savedUser.getRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet()));
        String refreshToken = issueRefreshToken(savedSession);
        LocalDateTime accessTokenExpiresAt = accessTokenIssuer.extractExpiry(accessToken);

        registrationSessionStore.deleteByRegId(command.regId());

        log.info("Registration completed for userId: {}, sessionId: {}",
                savedUser.getUserId(), savedSession.getSessionId());
        identityMetrics.registrationLifecycle("completed");
        return registrationResultMapper.toCompleteResult(
                savedSession,
                accessToken,
                refreshToken,
                accessTokenExpiresAt);
    }

    private void validateUniqueIdentity(String phoneNumber, String username) {
        if (userPersistencePort.existsByPhone(phoneNumber)) {
            throw new IdentityException(IdentityErrorCode.PHONE_ALREADY_EXISTS);
        }
        if (userPersistencePort.existsByUsername(username)) {
            throw new IdentityException(IdentityErrorCode.USERNAME_ALREADY_EXISTS);
        }
    }

    private PhoneNumber validateAndNormalizePhoneNumber(String identity) {
        try {
            PhoneNumber phoneNumber = PhoneNumber.of(identity);
            if (!phoneNumber.isE164()) {
                phoneNumber = PhoneNumber.of(phoneNumber.toE164(
                        registrationPolicy.getDefaultCountryCallingCode()));
            }
            return phoneNumber;
        } catch (IllegalArgumentException e) {
            throw new IdentityException(IdentityErrorCode.PHONE_INVALID);
        }
    }

    private void enforceRateLimits(String ipAddress, String phoneNumber) {
        boolean ipOk = registrationRateLimiter.check("IP", ipAddress,
                registrationPolicy.getIpRateLimitMaxAttempts(),
                registrationPolicy.getIpRateLimitWindowSeconds());
        boolean phoneOk = registrationRateLimiter.check("PHONE", phoneNumber,
                registrationPolicy.getPhoneRateLimitMaxAttempts(),
                registrationPolicy.getPhoneRateLimitWindowSeconds());
        if (!ipOk || !phoneOk) {
            throw new IdentityException(IdentityErrorCode.RATE_LIMIT_EXCEEDED);
        }
    }

    private IdentityUser createUser(String phoneNumber, String username, String password) {
        IdentityUser user = IdentityUser.createNew(
                IdGenerator.ulid(),
                null,
                phoneNumber,
                username);
        user.updatePasswordHash(passwordHasher.hash(password));
        user.updateDisplayName(username);
        user.verifyPhone();
        return user;
    }

    private AuthSession createAuthSession(String userId, String ipAddress, String userAgent) {
        return AuthSession.createNew(
                IdGenerator.ulid(),
                userId,
                ipAddress,
                userAgent,
                LocalDateTime.now().plusDays(registrationPolicy.getSessionExpiresDays()));
    }

    private String issueRefreshToken(AuthSession session) {
        byte[] bytes = new byte[com.aionn.identity.application.policy.IdentityValidationConstants.REFRESH_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        String tokenId = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        Duration ttl = Duration.between(LocalDateTime.now(), session.getExpiresAt());
        if (ttl.isNegative() || ttl.isZero()) {
            ttl = Duration.ofDays(registrationPolicy.getSessionExpiresDays());
        }
        refreshTokenStore.store(tokenId, session.getSessionId(), ttl);
        return tokenId;
    }
}
