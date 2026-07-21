package com.aionn.payment.infrastructure.provider.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentPropertiesConfigTest {

    @Test
    void shouldInstantiateSuccessfully() {
        PaymentPropertiesConfig config = new PaymentPropertiesConfig();
        assertThat(config).isNotNull();
    }
}
