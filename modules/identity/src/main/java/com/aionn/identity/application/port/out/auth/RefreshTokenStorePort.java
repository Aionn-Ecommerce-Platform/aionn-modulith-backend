package com.aionn.identity.application.port.out.auth;

import java.time.Duration;
import java.util.Optional;

public interface RefreshTokenStorePort {

    void store(String tokenId, String sessionId, Duration ttl);

    Optional<String> findSessionId(String tokenId);

    Optional<String> consumeSessionId(String tokenId);

    void revoke(String tokenId);

    void revokeBySessionId(String sessionId);
}
