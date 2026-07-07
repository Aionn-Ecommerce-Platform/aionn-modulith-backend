package com.aionn.identity.infrastructure.policy;

import com.aionn.identity.infrastructure.config.properties.AgentProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpringAgentPolicyTest {

    @Test
    void mapsKeyExpiryYears() {
        SpringAgentPolicy policy = new SpringAgentPolicy(new AgentProperties(3));

        assertThat(policy.getKeyExpiryYears()).isEqualTo(3);
    }
}
