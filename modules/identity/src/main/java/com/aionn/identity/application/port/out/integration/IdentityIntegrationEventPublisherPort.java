package com.aionn.identity.application.port.out.integration;

public interface IdentityIntegrationEventPublisherPort {

    void publishPasswordResetRequested(String userId, String resetToken);

    void publishPasswordChanged(String userId, String channelHint);

    void publishEmailChanged(String userId, String oldEmail, String newEmail);

    void publishPhoneChanged(String userId, String oldPhone, String newPhone);

    /**
     * Announces that a deletion request has completed its grace period and the account is tombstoned.
     * Consumers holding personal data keyed by this user ID must erase it; historical business records
     * keep the opaque ID.
     */
    void publishAccountDeleted(String userId);
}
