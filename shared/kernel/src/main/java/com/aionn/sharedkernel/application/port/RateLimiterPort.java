package com.aionn.sharedkernel.application.port;

public interface RateLimiterPort {

    boolean allow(String namespace, String key, int maxRequests, int windowSeconds);
}
