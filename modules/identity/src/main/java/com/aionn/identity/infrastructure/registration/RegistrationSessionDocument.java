package com.aionn.identity.infrastructure.registration;

import com.aionn.identity.domain.model.RegistrationVerificationSession;

import java.time.Instant;

public record RegistrationSessionDocument(
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

    static RegistrationSessionDocument from(RegistrationVerificationSession session) {
        return new RegistrationSessionDocument(
                session.getRegId(), session.getPhoneNumber(), session.getOtpCode(), session.getAttemptCount(),
                session.getMaxVerifyAttempts(), session.getResendAvailableAt(), session.getExpiredAt(),
                session.isVerified(), session.getVerificationToken(), session.getVerifiedAt());
    }

    RegistrationVerificationSession toDomain() {
        return new RegistrationVerificationSession(
                regId, phoneNumber, otpCode, attemptCount, maxVerifyAttempts, resendAvailableAt, expiredAt,
                verified, verificationToken, verifiedAt);
    }
}
