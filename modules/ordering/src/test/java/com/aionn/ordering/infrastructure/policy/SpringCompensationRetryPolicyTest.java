package com.aionn.ordering.infrastructure.policy;

import com.aionn.ordering.infrastructure.config.properties.OrderingCompensationProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpringCompensationRetryPolicyTest {

    @Test
    void exposesConfiguredMaxAttemptsAndBatchSize() {
        var policy = new SpringCompensationRetryPolicy(new OrderingCompensationProperties(8, 250));

        assertThat(policy.maxAttempts()).isEqualTo(8);
        assertThat(policy.batchSize()).isEqualTo(250);
    }
}
