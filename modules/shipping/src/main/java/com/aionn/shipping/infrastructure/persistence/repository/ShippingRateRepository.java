package com.aionn.shipping.infrastructure.persistence.repository;

import com.aionn.shipping.infrastructure.persistence.entity.ShippingRateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShippingRateRepository extends JpaRepository<ShippingRateEntity, String> {
    Optional<ShippingRateEntity> findByZoneCode(String zoneCode);
}

