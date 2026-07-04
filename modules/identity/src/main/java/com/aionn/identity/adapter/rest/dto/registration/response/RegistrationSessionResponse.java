package com.aionn.identity.adapter.rest.dto.registration.response;

import java.time.LocalDateTime;

// OTP code is intentionally NOT exposed here — it is delivered out-of-band
// (SMS/email). Application-layer results still carry it so the notification
// adapter can dispatch, but the REST response only surfaces session metadata.
public record RegistrationSessionResponse(
        String regId,
        LocalDateTime resendAvailableAt,
        LocalDateTime expiredAt) {
}
