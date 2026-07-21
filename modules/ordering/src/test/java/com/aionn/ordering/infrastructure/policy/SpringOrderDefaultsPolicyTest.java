package com.aionn.ordering.infrastructure.policy;

import com.aionn.ordering.application.policy.SpringOrderDefaultsPolicy;
import com.aionn.ordering.infrastructure.config.properties.OrderingDefaultsProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpringOrderDefaultsPolicyTest {

    @Mock
    private OrderingDefaultsProperties properties;

    @InjectMocks
    private SpringOrderDefaultsPolicy policy;

    @Test
    void returnsConfiguredCurrencyInUppercase() {
        when(properties.currency()).thenReturn("usd");

        String result = policy.defaultCurrency();

        assertThat(result).isEqualTo("USD");
    }

    @Test
    void trimsWhitespaceFromCurrency() {
        when(properties.currency()).thenReturn("  vnd  ");

        String result = policy.defaultCurrency();

        assertThat(result).isEqualTo("VND");
    }

    @Test
    void returnsVNDWhenCurrencyIsNull() {
        when(properties.currency()).thenReturn(null);

        String result = policy.defaultCurrency();

        assertThat(result).isEqualTo("VND");
    }

    @Test
    void returnsVNDWhenCurrencyIsEmpty() {
        when(properties.currency()).thenReturn("");

        String result = policy.defaultCurrency();

        assertThat(result).isEqualTo("VND");
    }

    @Test
    void returnsVNDWhenCurrencyIsBlank() {
        when(properties.currency()).thenReturn("   ");

        String result = policy.defaultCurrency();

        assertThat(result).isEqualTo("VND");
    }

    @Test
    void handlesMultipleCurrencyCodes() {
        when(properties.currency()).thenReturn("eur");
        assertThat(policy.defaultCurrency()).isEqualTo("EUR");

        when(properties.currency()).thenReturn("jpy");
        assertThat(policy.defaultCurrency()).isEqualTo("JPY");
    }
}
