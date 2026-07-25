package com.aionn.shipping.infrastructure.config;

import com.aionn.shipping.infrastructure.config.properties.ShippingDefaultsProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpringShippingDefaultsPolicyTest {

    @Test
    void shouldReturnConfiguredCurrencyInUpperCaseAndTrimmed() {
        ShippingDefaultsProperties properties = new ShippingDefaultsProperties(" usd ");
        SpringShippingDefaultsPolicy policy = new SpringShippingDefaultsPolicy(properties);

        assertThat(policy.defaultCurrency()).isEqualTo("USD");
    }

    @Test
    void shouldReturnDefaultCurrencyWhenNull() {
        ShippingDefaultsProperties properties = new ShippingDefaultsProperties(null);
        SpringShippingDefaultsPolicy policy = new SpringShippingDefaultsPolicy(properties);

        assertThat(policy.defaultCurrency()).isEqualTo("VND");
    }

    @Test
    void shouldReturnDefaultCurrencyWhenBlank() {
        ShippingDefaultsProperties properties = new ShippingDefaultsProperties("   ");
        SpringShippingDefaultsPolicy policy = new SpringShippingDefaultsPolicy(properties);

        assertThat(policy.defaultCurrency()).isEqualTo("VND");
    }
}
