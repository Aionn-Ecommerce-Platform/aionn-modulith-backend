package com.aionn.identity.application.dto.registration.result;

public record VerifyRegistrationOtpResult(
                String regId,
                String verificationToken) {
    @Override
    public String toString() {
        return "VerifyRegistrationOtpResult[regId=%s, verificationToken=***]".formatted(regId);
    }
}



