package com.aionn.identity.application.dto.registration.result;

import java.time.Instant;

public record InitiateRegistrationResult(
		String regId,
		Instant resendAvailableAt,
		Instant expiredAt,
		String otpCode) {
    @Override
    public String toString() {
        return "InitiateRegistrationResult[regId=%s, resendAvailableAt=%s, expiredAt=%s, otpCode=***]"
                .formatted(regId, resendAvailableAt, expiredAt);
    }
}



