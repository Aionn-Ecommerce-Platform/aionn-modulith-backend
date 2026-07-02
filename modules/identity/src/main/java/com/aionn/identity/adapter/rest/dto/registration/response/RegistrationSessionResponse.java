package com.aionn.identity.adapter.rest.dto.registration.response;

import java.time.LocalDateTime;

public record RegistrationSessionResponse(
        String regId,
        LocalDateTime resendAvailableAt,
        LocalDateTime expiredAt,
        String otpCode) {
}

