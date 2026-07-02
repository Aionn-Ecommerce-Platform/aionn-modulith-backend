package com.aionn.identity.application.dto.security.result;

public record MfaSetupResult(
        String secret,
        String otpauthUri,
        String issuer,
        String accountName) {
}
