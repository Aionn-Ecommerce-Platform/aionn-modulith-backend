package com.aionn.identity.application.port.out.security;

public interface AbuseRateLimiterPort {

    boolean check(String scope, String key, int maxAttempts, int windowSeconds);
}
