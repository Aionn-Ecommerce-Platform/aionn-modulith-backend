package com.aionn.shipping.application.port.out;

import com.aionn.shipping.domain.model.ShippingRate;

import java.util.Optional;

public interface ShippingRatePersistencePort {

    ShippingRate save(ShippingRate rate);

    Optional<ShippingRate> findById(String rateId);

    Optional<ShippingRate> findByZoneCode(String zoneCode);
}

