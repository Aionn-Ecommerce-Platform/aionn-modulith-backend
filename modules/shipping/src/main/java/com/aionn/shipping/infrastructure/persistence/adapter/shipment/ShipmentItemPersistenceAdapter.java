package com.aionn.shipping.infrastructure.persistence.adapter.shipment;

import com.aionn.shipping.application.port.out.ShipmentItemPersistencePort;
import com.aionn.shipping.infrastructure.persistence.entity.ShipmentItemEntity;
import com.aionn.shipping.infrastructure.persistence.repository.ShipmentItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ShipmentItemPersistenceAdapter implements ShipmentItemPersistencePort {

    private final ShipmentItemRepository repository;

    @Override
    public void saveAll(String shipmentId, String orderId, List<Line> lines) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        repository.saveAll(lines.stream()
                .map(line -> ShipmentItemEntity.builder()
                        .id(new ShipmentItemEntity.ShipmentItemId(shipmentId, line.skuId()))
                        .orderId(orderId)
                        .qty(line.qty())
                        .build())
                .toList());
    }
}
