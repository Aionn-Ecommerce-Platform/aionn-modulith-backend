package com.aionn.inventory.application.port.in.reservation;

import com.aionn.inventory.application.dto.reservation.command.CommitReservationCommand;
import com.aionn.inventory.application.dto.reservation.result.ReservationResult;

public interface CommitReservationInputPort {
    ReservationResult execute(CommitReservationCommand command);
}
