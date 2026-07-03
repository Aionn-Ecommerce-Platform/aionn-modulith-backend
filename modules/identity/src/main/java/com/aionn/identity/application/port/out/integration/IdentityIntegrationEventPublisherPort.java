package com.aionn.identity.application.port.out.integration;

public interface IdentityIntegrationEventPublisherPort {

    void publishPasswordChanged(String userId, String channelHint);

    void publishEmailChanged(String userId, String oldEmail, String newEmail);

    void publishPhoneChanged(String userId, String oldPhone, String newPhone);
}
