package com.aionn.shipping.infrastructure.persistence.adapter;

import com.aionn.shipping.application.port.out.shipment.ShipmentRepositoryPort;
import com.aionn.shipping.domain.model.Shipment;
import com.aionn.shipping.infrastructure.persistence.entity.ShipmentEntity;
import com.aionn.shipping.infrastructure.persistence.mapper.ShipmentDomainMapper;
import com.aionn.shipping.infrastructure.persistence.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ShipmentRepositoryPersistenceAdapter implements ShipmentRepositoryPort {

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
    public List<Shipment> findByStatus(String status, int limit) {
        return jpa.findByStatusOrderByCreatedAtDesc(status, PageRequest.of(0, Math.max(1, limit))).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
