package com.aionn.identity.infrastructure.policy;

import com.aionn.identity.application.policy.MfaConstants;
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
    public String mfaIssuer() {
        return properties.issuer();
    }

    @Override
    public int backupCodeCount() {
        return properties.backupCodeCount();
    }

    @Override
    public int backupCodeLength() {
        return properties.backupCodeLength();
    }

    @Override
    public int totpCodeLength() {
        return MfaConstants.TOTP_CODE_LENGTH;
    }
}
