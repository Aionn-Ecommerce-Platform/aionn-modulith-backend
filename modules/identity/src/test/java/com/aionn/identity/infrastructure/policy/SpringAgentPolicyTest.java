package com.aionn.identity.infrastructure.policy;

import com.aionn.identity.infrastructure.config.properties.AgentProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpringAgentPolicyTest {

    @Test
    void mapsConfiguredValues() {
        SpringAgentPolicy policy = new SpringAgentPolicy(new AgentProperties(3, 250));

        assertThat(policy.keyExpiryYears()).isEqualTo(3);
        assertThat(policy.auditLogLimit()).isEqualTo(250);
    }
}
