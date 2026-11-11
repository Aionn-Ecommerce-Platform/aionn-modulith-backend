package com.aionn.shipping.application.policy;

public interface CarrierWebhookSecurityPolicy {

    boolean isAuthorized(String providedSecret);

    /**
     * Verifies a signed webhook payload. Implementations that only support a legacy
     * shared secret may fall back to {@link #isAuthorized(String)} when signature data
     * is absent.
     */
    default boolean isAuthorized(String payload, String signature, String timestamp,
            String legacySecret) {
        return isAuthorized(legacySecret);
    }
}
