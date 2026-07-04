package com.aionn.identity.adapter.rest.dto.registration.response;

public record VerifyOtpResponse(
        String regId,
        String verificationToken
) {}


