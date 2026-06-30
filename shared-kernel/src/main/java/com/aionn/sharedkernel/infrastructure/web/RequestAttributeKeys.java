package com.aionn.sharedkernel.infrastructure.web;

public final class RequestAttributeKeys {

    public static final String CLIENT_IP = "clientIp";
    public static final String IDEMPOTENCY_ACTIVE = "idempotencyActive";
    public static final String IDEMPOTENCY_KEY = "idempotencyKey";
    public static final String IDEMPOTENCY_REQUEST_HASH = "idempotencyRequestHash";
    public static final String IDEMPOTENCY_TTL_SECONDS = "idempotencyTtlSeconds";
    public static final String IDEMPOTENCY_COMPLETED = "idempotencyCompleted";

    private RequestAttributeKeys() {
    }
}
