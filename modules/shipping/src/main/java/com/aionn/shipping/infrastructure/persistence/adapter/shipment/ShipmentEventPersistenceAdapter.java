package com.aionn.shipping.infrastructure.persistence.adapter.shipment;

import com.aionn.shipping.application.port.out.ShipmentEventPersistencePort;
import com.aionn.shipping.domain.valueobject.ShipmentTrackingEvent;
import com.aionn.shipping.infrastructure.persistence.entity.ShipmentEventEntity;
import com.aionn.shipping.infrastructure.persistence.repository.ShipmentEventRepository;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ShipmentEventPersistenceAdapter implements ShipmentEventPersistencePort {

    private final ShipmentEventRepository repository;

    @Override
    public ShipmentTrackingEvent record(String shipmentId, String status, String description, String location,
            Instant occurredAt) {
        ShipmentEventEntity saved = repository.save(ShipmentEventEntity.builder()
                .eventId(IdGenerator.ulid())
                .shipmentId(shipmentId)
                .status(status)
                .description(description)
                .location(location)
                .occurredAt(occurredAt)
                .build());
        return toEvent(saved);
    }

    @Override
    public List<ShipmentTrackingEvent> findByShipmentId(String shipmentId) {
        return repository.findByShipmentIdOrderByOccurredAtAsc(shipmentId).stream()
                .map(this::toEvent)
                .toList();
    }

    private ShipmentTrackingEvent toEvent(ShipmentEventEntity entity) {
        return new ShipmentTrackingEvent(
                entity.getEventId(),
                entity.getShipmentId(),
                entity.getStatus(),
                entity.getDescription(),
                entity.getLocation(),
                entity.getOccurredAt());
    }
}
