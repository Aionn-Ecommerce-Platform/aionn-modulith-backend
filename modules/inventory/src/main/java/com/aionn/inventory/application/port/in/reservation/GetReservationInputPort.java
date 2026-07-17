package com.aionn.inventory.application.port.in.reservation;

import com.aionn.inventory.application.dto.reservation.result.ReservationResult;

public interface GetReservationInputPort {
    ReservationResult execute(String reservationId);
}
