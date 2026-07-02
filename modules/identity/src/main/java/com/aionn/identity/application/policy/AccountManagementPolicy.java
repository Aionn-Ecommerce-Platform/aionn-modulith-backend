package com.aionn.identity.application.policy;

public interface AccountManagementPolicy {

    int getOtpExpirySeconds();

    int getOtpMaxAttempts();

    int getDeletionGraceDays();
}
