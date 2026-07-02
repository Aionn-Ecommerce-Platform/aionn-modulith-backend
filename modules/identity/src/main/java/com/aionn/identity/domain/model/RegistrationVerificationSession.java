package com.aionn.identity.domain.model;

import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.sharedkernel.util.IdGenerator;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public class RegistrationVerificationSession implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String regId;
    private final String phoneNumber;
    private String otpCode;
    private final int maxVerifyAttempts;
    private LocalDateTime resendAvailableAt;
    private LocalDateTime expiredAt;
    private int attemptCount;
    private boolean verified;
    private String verificationToken;
    private LocalDateTime verifiedAt;

    @JsonCreator
    public RegistrationVerificationSession(
            @JsonProperty("regId") String regId,
            @JsonProperty("phoneNumber") String phoneNumber,
            @JsonProperty("otpCode") String otpCode,
            @JsonProperty("attemptCount") int attemptCount,
            @JsonProperty("maxVerifyAttempts") int maxVerifyAttempts,
            @JsonProperty("resendAvailableAt") LocalDateTime resendAvailableAt,
            @JsonProperty("expiredAt") LocalDateTime expiredAt,
            @JsonProperty("verified") boolean verified,
            @JsonProperty("verificationToken") String verificationToken,
            @JsonProperty("verifiedAt") LocalDateTime verifiedAt) {
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

    public boolean isExpired() {
        return expiredAt != null && !expiredAt.isAfter(LocalDateTime.now());
    }

    public boolean isLocked() {
        return attemptCount >= maxVerifyAttempts;
    }

    public void verify(String inputOtpCode) {
        if (verified) {
            throw new IdentityException(IdentityErrorCode.REGISTRATION_ALREADY_VERIFIED);
        }
        if (isExpired()) {
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
        verifiedAt = LocalDateTime.now();
    }

    public void resend(String newOtpCode, LocalDateTime newResendAvailableAt, LocalDateTime newExpiredAt) {
        if (verified) {
            throw new IdentityException(IdentityErrorCode.REGISTRATION_ALREADY_VERIFIED);
        }
        if (isExpired()) {
            throw new IdentityException(IdentityErrorCode.REGISTRATION_SESSION_EXPIRED);
        }
        if (isLocked()) {
            throw new IdentityException(IdentityErrorCode.OTP_ATTEMPTS_EXCEEDED);
        }
        if (resendAvailableAt != null && LocalDateTime.now().isBefore(resendAvailableAt)) {
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

    public LocalDateTime getResendAvailableAt() {
        return resendAvailableAt;
    }

    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }

    public boolean isVerified() {
        return verified;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }
}
