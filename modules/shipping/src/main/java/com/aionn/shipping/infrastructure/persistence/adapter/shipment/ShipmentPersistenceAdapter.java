package com.aionn.shipping.infrastructure.persistence.adapter.shipment;

import com.aionn.shipping.application.port.out.ShipmentPersistencePort;
import com.aionn.shipping.domain.model.Shipment;
import com.aionn.shipping.infrastructure.persistence.entity.ShipmentEntity;
import com.aionn.shipping.infrastructure.persistence.mapper.ShipmentDomainMapper;
import com.aionn.shipping.infrastructure.persistence.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class ShipmentPersistenceAdapter implements ShipmentPersistencePort {

    private static final Set<String> TERMINAL = Set.of("DELIVERED", "RETURNED", "CANCELLED");

    private final ShipmentRepository jpa;
    private final ShipmentDomainMapper mapper;

    @Override
    public Shipment save(Shipment shipment) {
        ShipmentEntity existing = jpa.findById(shipment.getShipmentId()).orElse(null);
        return mapper.toDomain(jpa.save(mapper.toEntity(shipment, existing)));
    }

    @Override
    public Optional<Shipment> findById(String shipmentId) {
        return jpa.findById(shipmentId).map(mapper::toDomain);
    }

    @Override
    public Optional<Shipment> findByTrackingCode(String trackingCode) {
        return jpa.findByTrackingCode(trackingCode).map(mapper::toDomain);
    }

    @Override
    public List<Shipment> findByOrderId(String orderId) {
        return jpa.findByOrderId(orderId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Shipment> findActiveTracking(int batchSize) {
        return jpa.findByTrackingCodeIsNotNullAndStatusNotIn(TERMINAL,
                PageRequest.of(0, Math.max(1, batchSize)))
                .stream().map(mapper::toDomain).toList();
    }
}
