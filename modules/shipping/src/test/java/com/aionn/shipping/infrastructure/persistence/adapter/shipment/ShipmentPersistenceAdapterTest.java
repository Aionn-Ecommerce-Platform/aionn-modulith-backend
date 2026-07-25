package com.aionn.shipping.infrastructure.persistence.adapter.shipment;

import com.aionn.shipping.domain.model.Shipment;
import com.aionn.shipping.infrastructure.persistence.entity.ShipmentEntity;
import com.aionn.shipping.infrastructure.persistence.mapper.ShipmentDomainMapper;
import com.aionn.shipping.infrastructure.persistence.repository.ShipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentPersistenceAdapterTest {

    @Mock
    private ShipmentRepository jpa;

    @Mock
    private ShipmentDomainMapper mapper;

    @Mock
    private Shipment shipment;

    @Mock
    private ShipmentEntity entity;

    private ShipmentPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ShipmentPersistenceAdapter(jpa, mapper);
    }

    @Test
    void saveSavesAndReturnsDomain() {
        when(shipment.getShipmentId()).thenReturn("S_1");
        when(jpa.findById("S_1")).thenReturn(Optional.empty());
        when(mapper.toEntity(shipment, null)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(shipment);

        Shipment result = adapter.save(shipment);

        assertThat(result).isEqualTo(shipment);
        verify(jpa).save(entity);
    }

    @Test
    void findByIdReturnsDomainOptional() {
        when(jpa.findById("S_1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(shipment);

        Optional<Shipment> result = adapter.findById("S_1");

        assertThat(result).contains(shipment);
    }

    @Test
    void findByTrackingCodeReturnsDomainOptional() {
        when(jpa.findByTrackingCode("TR_1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(shipment);

        Optional<Shipment> result = adapter.findByTrackingCode("TR_1");

        assertThat(result).contains(shipment);
    }

    @Test
    void findByOrderIdReturnsList() {
        when(jpa.findByOrderId("O_1")).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(shipment);

        List<Shipment> result = adapter.findByOrderId("O_1");

        assertThat(result).containsExactly(shipment);
    }

    @Test
    void findActiveTrackingReturnsList() {
        when(jpa.findByTrackingCodeIsNotNullAndStatusNotIn(any(), any(Pageable.class)))
                .thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(shipment);

        List<Shipment> result = adapter.findActiveTracking(10);

        assertThat(result).containsExactly(shipment);
    }
}
