package com.aionn.shipping.application.policy;

public interface CarrierWebhookSecurityPolicy {

    boolean isAuthorized(String providedSecret);

    default boolean isAuthorized(byte[] payload, String signature, String timestamp,
            String legacySecret) {
        return isAuthorized(legacySecret);
    }
}
