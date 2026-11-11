package com.aionn.inventory.infrastructure.policy;

import com.aionn.inventory.infrastructure.config.InventoryProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpringInventoryReservationPolicyTest {

    @Test
    void getDefaultTtlSecondsReturnsConfiguredValue() {
        var properties = properties(600, 100);
        var policy = new SpringInventoryReservationPolicy(properties);

        assertThat(policy.getDefaultTtlSeconds()).isEqualTo(600);
    }

    @Test
    void policyDelegatesToPropertiesCorrectly() {
        var properties = properties(1800, 200);
        var policy = new SpringInventoryReservationPolicy(properties);

        assertThat(policy.getDefaultTtlSeconds()).isEqualTo(1800);
    }

    private static InventoryProperties properties(int defaultTtlSeconds, int batchSize) {
        return new InventoryProperties(new InventoryProperties.Reservation(
                defaultTtlSeconds,
                new InventoryProperties.Reservation.AutoRelease(true, 30_000, batchSize)));
    }
}
