package com.aionn.identity.application.dto.registration.result;

import java.time.LocalDateTime;

public record ResendRegistrationOtpResult(
                String regId,
                LocalDateTime resendAvailableAt,
                LocalDateTime expiredAt,
                String otpCode) {
}



