package com.aionn.identity.application.policy;

public interface AccountManagementPolicy {

    int otpExpirySeconds();

    int otpMaxAttempts();

    int deletionGraceDays();
}
