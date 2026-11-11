package com.aionn.identity.application.policy;

public interface MfaCodeFormatPolicy {

    int totpCodeLength();

    int backupCodeLength();
}
