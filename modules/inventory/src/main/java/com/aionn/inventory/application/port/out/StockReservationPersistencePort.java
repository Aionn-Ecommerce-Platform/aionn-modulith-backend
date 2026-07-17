package com.aionn.inventory.application.port.out;

import com.aionn.inventory.domain.model.StockReservation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface StockReservationPersistencePort {

    StockReservation save(StockReservation reservation);

    Optional<StockReservation> findById(String reservationId);

    List<StockReservation> findExpired(Instant now, int limit);
}
