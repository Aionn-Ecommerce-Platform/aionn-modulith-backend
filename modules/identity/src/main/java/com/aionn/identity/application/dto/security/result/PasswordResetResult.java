package com.aionn.identity.application.dto.security.result;

public record PasswordResetResult(String message) {

    public static PasswordResetResult acceptedResult() {
        return new PasswordResetResult("Password reset requested");
    }
}
