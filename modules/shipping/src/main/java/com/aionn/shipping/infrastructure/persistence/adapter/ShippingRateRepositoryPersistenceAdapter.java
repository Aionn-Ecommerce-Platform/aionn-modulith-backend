package com.aionn.shipping.infrastructure.persistence.adapter;

import com.aionn.shipping.application.port.out.rate.ShippingRateRepositoryPort;
import com.aionn.shipping.domain.model.ShippingRate;
import com.aionn.shipping.infrastructure.persistence.entity.ShippingRateEntity;
import com.aionn.shipping.infrastructure.persistence.mapper.ShippingRateDomainMapper;
import com.aionn.shipping.infrastructure.persistence.repository.ShippingRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ShippingRateRepositoryPersistenceAdapter implements ShippingRateRepositoryPort {

    private final ShippingRateRepository jpa;
    private final ShippingRateDomainMapper mapper;

    @Override
    public ShippingRate save(ShippingRate rate) {
        ShippingRateEntity existing = jpa.findById(rate.getRateId()).orElse(null);
        return mapper.toDomain(jpa.save(mapper.toEntity(rate, existing)));
    }

    @Override
    public Optional<ShippingRate> findById(String rateId) {
        return jpa.findById(rateId).map(mapper::toDomain);
    }

    @Override
    public Optional<ShippingRate> findByZoneCode(String zoneCode) {
        return jpa.findByZoneCode(zoneCode).map(mapper::toDomain);
    }

    @Override
    public List<ShippingRate> findAll() {
        return jpa.findAll(Sort.by(Sort.Direction.ASC, "zoneCode")).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
