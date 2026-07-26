package com.aionn.shipping.infrastructure.carrier;

import com.aionn.shipping.domain.valueobject.ShipmentStatus;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

@Component
public class GhnStatusMapper {

    public Optional<ShipmentStatus> map(String ghnStatus) {
        if (ghnStatus == null || ghnStatus.isBlank()) {
            return Optional.empty();
        }
        return switch (ghnStatus.trim().toLowerCase(Locale.ROOT)) {
            case "ready_to_pick", "picking", "money_collect_picking" -> Optional.of(ShipmentStatus.REGISTERED);
            case "picked" -> Optional.of(ShipmentStatus.PICKED_UP);
            case "storing", "transporting", "sorting" -> Optional.of(ShipmentStatus.IN_TRANSIT);
            case "delivering", "money_collect_delivering" -> Optional.of(ShipmentStatus.OUT_FOR_DELIVERY);
            case "delivered" -> Optional.of(ShipmentStatus.DELIVERED);
            case "delivery_fail", "waiting_to_return" -> Optional.of(ShipmentStatus.DELIVERY_FAILED);
            case "return", "returning", "return_transporting", "return_sorting", "returned",
                    "return_fail" ->
                Optional.of(ShipmentStatus.RETURNED);
            case "cancel" -> Optional.of(ShipmentStatus.CANCELLED);
            default -> Optional.empty();
        };
    }
}
