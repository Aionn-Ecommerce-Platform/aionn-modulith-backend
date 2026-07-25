package com.aionn.shipping.application.port.out.rate;

import com.aionn.shipping.domain.model.ShippingRate;

import java.util.List;
import java.util.Optional;

public interface ShippingRateRepositoryPort {

    ShippingRate save(ShippingRate rate);

    Optional<ShippingRate> findById(String rateId);

    Optional<ShippingRate> findByZoneCode(String zoneCode);

    List<ShippingRate> findAll();
}
