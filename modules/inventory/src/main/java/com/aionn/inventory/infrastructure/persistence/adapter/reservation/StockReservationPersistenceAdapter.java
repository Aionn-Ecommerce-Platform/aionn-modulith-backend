package com.aionn.inventory.infrastructure.persistence.adapter.reservation;

import com.aionn.inventory.application.port.out.StockReservationPersistencePort;
import com.aionn.inventory.domain.model.StockReservation;
import com.aionn.inventory.infrastructure.persistence.entity.StockReservationEntity;
import com.aionn.inventory.infrastructure.persistence.mapper.StockReservationDomainMapper;
import com.aionn.inventory.infrastructure.persistence.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StockReservationPersistenceAdapter implements StockReservationPersistencePort {

    private final StockReservationRepository jpa;
    private final StockReservationDomainMapper mapper;

    @Override
    public StockReservation save(StockReservation reservation) {
        StockReservationEntity existing = jpa.findById(reservation.getReservationId()).orElse(null);
        StockReservationEntity entity = mapper.toEntity(reservation, existing);
        return mapper.toDomain(jpa.save(entity));
    }

    @Override
    public Optional<StockReservation> findById(String reservationId) {
        return jpa.findById(reservationId).map(mapper::toDomain);
    }

    @Override
    public List<StockReservation> findExpired(Instant now, int limit) {
        return jpa.findExpired(now, PageRequest.of(0, Math.max(1, limit))).stream()
                .map(mapper::toDomain)
                .toList();
    }
}

