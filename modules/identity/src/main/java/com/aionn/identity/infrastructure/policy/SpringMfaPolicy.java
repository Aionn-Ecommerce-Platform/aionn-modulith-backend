package com.aionn.identity.infrastructure.policy;

import com.aionn.identity.application.policy.IdentityValidationConstants;
import com.aionn.identity.application.policy.MfaCodeFormatPolicy;
import com.aionn.identity.application.policy.MfaPolicy;
import com.aionn.identity.infrastructure.config.properties.MfaProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringMfaPolicy implements MfaPolicy, MfaCodeFormatPolicy {

    private final MfaProperties properties;

    @Override
    public String getMfaIssuer() {
        return properties.issuer();
    }

    @Override
    public int getBackupCodeCount() {
        return properties.backupCodeCount();
    }

    @Override
    public int getBackupCodeLength() {
        return properties.backupCodeLength();
    }

    @Override
    public int backupCodeLength() {
        return properties.backupCodeLength();
    }

    @Override
    public int totpCodeLength() {
        return IdentityValidationConstants.TOTP_CODE_LENGTH;
    }
}
