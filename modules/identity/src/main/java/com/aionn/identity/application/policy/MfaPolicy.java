package com.aionn.identity.application.policy;

public interface MfaPolicy {

    String getMfaIssuer();

    int getBackupCodeCount();
}
