package com.aionn.identity.application.policy;

public interface MfaPolicy {

    String mfaIssuer();

    int backupCodeCount();

    int backupCodeLength();
}
