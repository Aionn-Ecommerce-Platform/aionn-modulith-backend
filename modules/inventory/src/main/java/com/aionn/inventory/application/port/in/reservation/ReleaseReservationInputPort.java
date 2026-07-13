package com.aionn.inventory.application.port.in.reservation;

import com.aionn.inventory.application.dto.reservation.command.ReleaseReservationCommand;
import com.aionn.inventory.application.dto.reservation.result.ReservationResult;

public interface ReleaseReservationInputPort {
    ReservationResult execute(ReleaseReservationCommand command);
}
