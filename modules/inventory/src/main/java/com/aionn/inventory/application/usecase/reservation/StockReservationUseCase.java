package com.aionn.inventory.application.usecase.reservation;

import com.aionn.inventory.application.dto.reservation.command.CommitReservationCommand;
import com.aionn.inventory.application.dto.reservation.command.ReleaseReservationCommand;
import com.aionn.inventory.application.dto.reservation.command.ReserveStockCommand;
import com.aionn.inventory.application.dto.reservation.result.ReservationResult;
import com.aionn.inventory.application.port.in.reservation.CommitReservationInputPort;
import com.aionn.inventory.application.port.in.reservation.GetReservationInputPort;
import com.aionn.inventory.application.port.in.reservation.ReleaseReservationInputPort;
import com.aionn.inventory.application.port.in.reservation.ReserveStockInputPort;
import com.aionn.inventory.application.service.StockReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockReservationUseCase implements
        ReserveStockInputPort,
        CommitReservationInputPort,
        ReleaseReservationInputPort,
        GetReservationInputPort {

    private final StockReservationService service;

    @Override
    public ReservationResult execute(ReserveStockCommand command) {
        return service.reserve(command);
    }

    @Override
    public ReservationResult execute(CommitReservationCommand command) {
        return service.commit(command);
    }

    @Override
    public ReservationResult execute(ReleaseReservationCommand command) {
        return service.release(command);
    }

    @Override
    public ReservationResult execute(String reservationId) {
        return service.get(reservationId);
    }
}
