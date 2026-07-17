package com.aionn.inventory.application.port.out.reservation;

import com.aionn.inventory.domain.model.StockReservation;
import com.aionn.inventory.domain.valueobject.ReservationStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface StockReservationRepositoryPort {

    StockReservation save(StockReservation reservation);

    Optional<StockReservation> findById(String reservationId);

    List<StockReservation> findByOrderId(String orderId);

    List<StockReservation> findByStatus(ReservationStatus status, int page, int size);

    List<StockReservation> findExpired(Instant now, int limit);
}
