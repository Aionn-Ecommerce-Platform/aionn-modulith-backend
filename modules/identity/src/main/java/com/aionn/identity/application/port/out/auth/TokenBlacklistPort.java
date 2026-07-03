package com.aionn.identity.application.port.out.auth;

import java.time.Duration;

public interface TokenBlacklistPort {

    void blacklist(String jti, Duration ttl);

    boolean isBlacklisted(String jti);
}
