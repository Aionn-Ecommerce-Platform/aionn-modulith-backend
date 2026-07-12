package com.aionn.identity.adapter.rest.dto.registration.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RegistrationSessionResponse(
        String regId,
        Instant resendAvailableAt,
        Instant expiredAt,
        String otpCode) {
}
