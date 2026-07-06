package com.aionn.identity.infrastructure.integration;

import com.aionn.identity.application.port.out.auth.AccessTokenClaims;
import com.aionn.identity.application.port.out.auth.AccessTokenIssuerPort;
import com.aionn.identity.application.port.out.auth.AuthSessionPersistencePort;
import com.aionn.identity.application.port.out.auth.TokenBlacklistPort;
import com.aionn.identity.domain.model.AuthSession;
import com.aionn.identity.domain.valueobject.AuthSessionStatus;
import com.aionn.sharedkernel.integration.port.identity.AccessTokenVerifierPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdentityAccessTokenVerifierAdapter implements AccessTokenVerifierPort {

    private static final String BEARER = "Bearer ";

    private final AccessTokenIssuerPort tokenIssuer;
    private final AuthSessionPersistencePort authSessionPersistencePort;
    private final TokenBlacklistPort tokenBlacklist;

    @Override
    public Optional<String> verifyAndExtractUserId(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER)) {
            return Optional.empty();
        }
        String token = authorizationHeader.substring(BEARER.length()).trim();
        Optional<AccessTokenClaims> parsed = tokenIssuer.parseClaims(token);
        if (parsed.isEmpty()) {
            return Optional.empty();
        }
        AccessTokenClaims claims = parsed.get();
        String sessionId = claims.sessionId();
        String userId = claims.userId();
        String jti = claims.jti();
        if (sessionId == null || userId == null) {
            return Optional.empty();
        }
        if (jti != null && tokenBlacklist.isBlacklisted(jti)) {
            return Optional.empty();
        }
        Optional<AuthSession> sessionOpt = authSessionPersistencePort.findById(sessionId);
        if (sessionOpt.isEmpty()
                || !AuthSessionStatus.ACTIVE.equals(sessionOpt.get().getStatus())
                || sessionOpt.get().isExpired()
                || !userId.equals(sessionOpt.get().getUserId())) {
            return Optional.empty();
        }
        return Optional.of(userId);
    }
}
