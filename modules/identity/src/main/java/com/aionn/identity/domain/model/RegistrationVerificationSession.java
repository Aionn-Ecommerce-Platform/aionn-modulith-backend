package com.aionn.identity.domain.model;

import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.sharedkernel.util.IdGenerator;
import java.io.Serializable;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public class RegistrationVerificationSession implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String regId;
    private final String phoneNumber;
    private String otpCode;
    private final int maxVerifyAttempts;
    private Instant resendAvailableAt;
    private Instant expiredAt;
    private int attemptCount;
    private boolean verified;
    private String verificationToken;
    private Instant verifiedAt;

    public RegistrationVerificationSession(
            String regId,
            String phoneNumber,
            String otpCode,
            int attemptCount,
            int maxVerifyAttempts,
            Instant resendAvailableAt,
            Instant expiredAt,
            boolean verified,
            String verificationToken,
            Instant verifiedAt) {
        this.regId = regId;
        this.phoneNumber = phoneNumber;
        this.otpCode = otpCode;
        this.attemptCount = attemptCount;
        this.maxVerifyAttempts = maxVerifyAttempts;
        this.resendAvailableAt = resendAvailableAt;
        this.expiredAt = expiredAt;
        this.verified = verified;
        this.verificationToken = verificationToken;
        this.verifiedAt = verifiedAt;
    }

    public boolean isExpired(Clock clock) {
        return expiredAt != null && !expiredAt.isAfter(clock.instant());
    }

    public boolean isLocked() {
        return attemptCount >= maxVerifyAttempts;
    }

    public void verify(String inputOtpCode, Clock clock) {
        if (verified) {
            throw new IdentityException(IdentityErrorCode.REGISTRATION_ALREADY_VERIFIED);
        }
        if (isExpired(clock)) {
            throw new IdentityException(IdentityErrorCode.OTP_EXPIRED);
        }
        if (isLocked()) {
            throw new IdentityException(IdentityErrorCode.OTP_ATTEMPTS_EXCEEDED);
        }
        if (inputOtpCode == null || inputOtpCode.isBlank() || !Objects.equals(otpCode, inputOtpCode)) {
            attemptCount++;
            if (isLocked()) {
                throw new IdentityException(IdentityErrorCode.OTP_ATTEMPTS_EXCEEDED);
            }
            throw new IdentityException(IdentityErrorCode.OTP_INVALID);
        }

        verified = true;
        otpCode = null;
        verificationToken = IdGenerator.ulid();
        verifiedAt = clock.instant();
    }

    public void resend(String newOtpCode, Instant newResendAvailableAt, Instant newExpiredAt, Clock clock) {
        if (verified) {
            throw new IdentityException(IdentityErrorCode.REGISTRATION_ALREADY_VERIFIED);
        }
        if (isExpired(clock)) {
            throw new IdentityException(IdentityErrorCode.REGISTRATION_SESSION_EXPIRED);
        }
        if (isLocked()) {
            throw new IdentityException(IdentityErrorCode.OTP_ATTEMPTS_EXCEEDED);
        }
        if (resendAvailableAt != null && clock.instant().isBefore(resendAvailableAt)) {
            throw new IdentityException(IdentityErrorCode.OTP_RESEND_TOO_SOON);
        }

        otpCode = newOtpCode;
        resendAvailableAt = newResendAvailableAt;
        expiredAt = newExpiredAt;
        attemptCount = 0;
    }

    public String getRegId() {
        return regId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public int getMaxVerifyAttempts() {
        return maxVerifyAttempts;
    }

    public Instant getResendAvailableAt() {
        return resendAvailableAt;
    }

    public Instant getExpiredAt() {
        return expiredAt;
    }

    public boolean isVerified() {
        return verified;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }
}
