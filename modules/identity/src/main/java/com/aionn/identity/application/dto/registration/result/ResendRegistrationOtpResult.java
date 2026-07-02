package com.aionn.identity.application.dto.registration.result;

import java.time.LocalDateTime;

public record ResendRegistrationOtpResult(
                String regId,
                LocalDateTime resendAvailableAt,
                LocalDateTime expiredAt,
                String otpCode) {
    @Override
    public String toString() {
        return "ResendRegistrationOtpResult[regId=%s, resendAvailableAt=%s, expiredAt=%s, otpCode=***]"
                .formatted(regId, resendAvailableAt, expiredAt);
    }
}



