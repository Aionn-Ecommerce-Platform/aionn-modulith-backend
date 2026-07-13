package com.aionn.inventory.application.port.in.reservation;

import com.aionn.inventory.application.dto.reservation.command.ReserveStockCommand;
import com.aionn.inventory.application.dto.reservation.result.ReservationResult;

public interface ReserveStockInputPort {
    ReservationResult execute(ReserveStockCommand command);
}
