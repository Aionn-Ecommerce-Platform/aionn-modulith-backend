package com.aionn.shipping.infrastructure.persistence.adapter.rate;

import com.aionn.shipping.domain.model.ShippingRate;
import com.aionn.shipping.infrastructure.persistence.entity.ShippingRateEntity;
import com.aionn.shipping.infrastructure.persistence.mapper.ShippingRateDomainMapper;
import com.aionn.shipping.infrastructure.persistence.repository.ShippingRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShippingRatePersistenceAdapterTest {

    @Mock
    private ShippingRateRepository jpa;

    @Mock
    private ShippingRateDomainMapper mapper;

    @Mock
    private ShippingRate rate;

    @Mock
    private ShippingRateEntity entity;

    private ShippingRatePersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ShippingRatePersistenceAdapter(jpa, mapper);
    }

    @Test
    void saveSavesAndReturnsDomain() {
        when(rate.getRateId()).thenReturn("R_1");
        when(jpa.findById("R_1")).thenReturn(Optional.empty());
        when(mapper.toEntity(rate, null)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(rate);

        ShippingRate result = adapter.save(rate);

        assertThat(result).isEqualTo(rate);
        verify(jpa).save(entity);
    }

    @Test
    void findByIdReturnsDomainOptional() {
        when(jpa.findById("R_1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(rate);

        Optional<ShippingRate> result = adapter.findById("R_1");

        assertThat(result).contains(rate);
    }

    @Test
    void findByZoneCodeReturnsDomainOptional() {
        when(jpa.findByZoneCode("HN")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(rate);

        Optional<ShippingRate> result = adapter.findByZoneCode("HN");

        assertThat(result).contains(rate);
    }
}
