package com.aionn.identity.infrastructure.policy;

import com.aionn.identity.infrastructure.config.properties.AccountManagementProperties;
import com.aionn.identity.infrastructure.config.properties.AccountManagementProperties.Deletion;
import com.aionn.identity.infrastructure.config.properties.AccountManagementProperties.Otp;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpringAccountManagementPolicyTest {

    @Test
    void mapsNestedOtpAndDeletionValues() {
        AccountManagementProperties properties = new AccountManagementProperties(new Otp(120, 4), new Deletion(15));

        SpringAccountManagementPolicy policy = new SpringAccountManagementPolicy(properties);

        assertThat(policy.otpExpirySeconds()).isEqualTo(120);
        assertThat(policy.otpMaxAttempts()).isEqualTo(4);
        assertThat(policy.deletionGraceDays()).isEqualTo(15);
    }
}
