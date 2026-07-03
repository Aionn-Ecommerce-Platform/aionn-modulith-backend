package com.aionn.identity.application.port.out.auth;

import java.util.List;

public record AccessTokenClaims(
        String userId,
        String sessionId,
        String jti,
        List<String> roles) {

    public AccessTokenClaims {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
