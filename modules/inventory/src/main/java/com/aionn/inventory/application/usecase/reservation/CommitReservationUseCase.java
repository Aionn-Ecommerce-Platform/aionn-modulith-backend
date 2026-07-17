package com.aionn.inventory.application.usecase.reservation;

import com.aionn.inventory.application.dto.reservation.command.CommitReservationCommand;
import com.aionn.inventory.application.dto.reservation.result.ReservationResult;
import com.aionn.inventory.application.port.in.reservation.CommitReservationInputPort;
import com.aionn.inventory.application.service.StockReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommitReservationUseCase implements CommitReservationInputPort {

    private final StockReservationService stockReservationService;

    @Override
    @Transactional
    public ReservationResult execute(CommitReservationCommand command) {
        return stockReservationService.commit(command);
    }
}
