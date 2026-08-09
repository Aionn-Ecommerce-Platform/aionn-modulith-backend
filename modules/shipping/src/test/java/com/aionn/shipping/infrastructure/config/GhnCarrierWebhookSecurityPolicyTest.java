package com.aionn.shipping.infrastructure.config;

import com.aionn.shipping.infrastructure.carrier.config.GhnProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GhnCarrierWebhookSecurityPolicyTest {

    private final GhnProperties properties = mock(GhnProperties.class);
    private final GhnCarrierWebhookSecurityPolicy policy = new GhnCarrierWebhookSecurityPolicy(properties);

    @Test
    void rejectsWebhookWhenNoSecretIsConfigured() {
        when(properties.webhookSecret()).thenReturn("  ");

        assertThat(policy.isAuthorized(null)).isFalse();
        assertThat(policy.isAuthorized("any-secret")).isFalse();
    }

    @Test
    void requiresExactMatchWhenSecretIsConfigured() {
        when(properties.webhookSecret()).thenReturn("expected-secret");

        assertThat(policy.isAuthorized("expected-secret")).isTrue();
        assertThat(policy.isAuthorized("wrong-secret")).isFalse();
        assertThat(policy.isAuthorized(null)).isFalse();
    }
}
