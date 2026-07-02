package com.aionn.identity.application.dto.registration.result;

public record VerifyRegistrationOtpResult(
                String regId,
                String verificationToken) {
}



