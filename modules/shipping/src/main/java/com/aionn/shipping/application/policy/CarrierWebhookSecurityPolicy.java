package com.aionn.shipping.application.policy;

public interface CarrierWebhookSecurityPolicy {

    boolean isAuthorized(String providedSecret);
}
