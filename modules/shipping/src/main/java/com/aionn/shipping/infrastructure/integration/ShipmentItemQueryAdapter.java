package com.aionn.shipping.infrastructure.integration;

import com.aionn.contracts.port.shipping.ShipmentItemQueryPort;
import com.aionn.shipping.infrastructure.persistence.repository.ShipmentItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

@Component("shippingShipmentItemQueryAdapter")
@RequiredArgsConstructor
public class ShipmentItemQueryAdapter implements ShipmentItemQueryPort {

    private final ShipmentItemRepository repository;

    @Override
    public List<DeliveredItem> deliveredItems(String orderId) {
        return repository.findDeliveredByOrderId(orderId).stream()
                .map(row -> new DeliveredItem(
                        (String) row[0],
                        ((Number) row[1]).intValue(),
                        toInstant(row[2])))
                .toList();
    }

    private static Instant toInstant(Object value) {
        if (value == null || value instanceof Instant) {
            return (Instant) value;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toInstant();
        }
        throw new IllegalStateException("Unsupported delivered timestamp type: " + value.getClass());
    }
}
