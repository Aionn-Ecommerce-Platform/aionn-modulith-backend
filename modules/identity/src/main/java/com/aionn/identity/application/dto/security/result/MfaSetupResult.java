package com.aionn.identity.application.dto.security.result;

public record MfaSetupResult(
        String secret,
        String otpauthUri,
        String issuer,
        String accountName) {
    @Override
    public String toString() {
        return "MfaSetupResult[secret=***, otpauthUri=***, issuer=%s, accountName=%s]"
                .formatted(issuer, accountName);
    }
}
