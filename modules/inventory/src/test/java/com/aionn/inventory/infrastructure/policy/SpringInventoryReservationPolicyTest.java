package com.aionn.inventory.infrastructure.policy;

import com.aionn.inventory.infrastructure.config.properties.InventoryReservationProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpringInventoryReservationPolicyTest {

    @Test
    void getDefaultTtlSecondsReturnsConfiguredValue() {
        var properties = new InventoryReservationProperties(600, 100,
                new InventoryReservationProperties.AutoRelease(true, 30000, 100));
        var policy = new SpringInventoryReservationPolicy(properties);

        assertThat(policy.getDefaultTtlSeconds()).isEqualTo(600);
    }

    @Test
    void getAutoReleaseBatchSizeReturnsConfiguredValue() {
        var properties = new InventoryReservationProperties(300, 50,
                new InventoryReservationProperties.AutoRelease(true, 30000, 50));
        var policy = new SpringInventoryReservationPolicy(properties);

        assertThat(policy.getAutoReleaseBatchSize()).isEqualTo(50);
    }

    @Test
    void policyDelegatesToPropertiesCorrectly() {
        var properties = new InventoryReservationProperties(1800, 200,
                new InventoryReservationProperties.AutoRelease(true, 30000, 200));
        var policy = new SpringInventoryReservationPolicy(properties);

        assertThat(policy.getDefaultTtlSeconds()).isEqualTo(1800);
        assertThat(policy.getAutoReleaseBatchSize()).isEqualTo(200);
    }
}
