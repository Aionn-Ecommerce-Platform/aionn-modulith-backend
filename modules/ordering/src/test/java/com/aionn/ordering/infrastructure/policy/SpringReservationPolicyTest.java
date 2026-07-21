package com.aionn.ordering.infrastructure.policy;

import com.aionn.ordering.application.policy.SpringReservationPolicy;
import com.aionn.ordering.infrastructure.config.properties.OrderingReservationProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpringReservationPolicyTest {

    @Mock
    private OrderingReservationProperties properties;

    @InjectMocks
    private SpringReservationPolicy policy;

    @Test
    void returnsTtlSecondsFromProperties() {
        when(properties.ttlSeconds()).thenReturn(300);

        int result = policy.ttlSeconds();

        assertThat(result).isEqualTo(300);
    }

    @Test
    void enforcesMinimumTtlOf60Seconds() {
        when(properties.ttlSeconds()).thenReturn(30);

        int result = policy.ttlSeconds();

        assertThat(result).isEqualTo(60);
    }

    @Test
    void enforcesMinimumTtlForZeroValue() {
        when(properties.ttlSeconds()).thenReturn(0);

        int result = policy.ttlSeconds();

        assertThat(result).isEqualTo(60);
    }

    @Test
    void enforcesMinimumTtlForNegativeValue() {
        when(properties.ttlSeconds()).thenReturn(-100);

        int result = policy.ttlSeconds();

        assertThat(result).isEqualTo(60);
    }

    @Test
    void allowsLargeTtlValues() {
        when(properties.ttlSeconds()).thenReturn(3600);

        int result = policy.ttlSeconds();

        assertThat(result).isEqualTo(3600);
    }
}
