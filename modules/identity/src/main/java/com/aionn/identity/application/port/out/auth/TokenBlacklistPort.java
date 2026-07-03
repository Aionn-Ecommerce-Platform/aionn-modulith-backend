package com.aionn.identity.application.port.out.auth;

public interface TokenBlacklistPort {

    void blacklist(String jti, long ttlSeconds);

    boolean isBlacklisted(String jti);
}
