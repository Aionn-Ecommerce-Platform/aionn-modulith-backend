package com.aionn.inventory.application.usecase.reservation;

import com.aionn.inventory.application.dto.reservation.command.ReserveStockCommand;
import com.aionn.inventory.application.dto.reservation.result.ReservationResult;
import com.aionn.inventory.application.port.in.reservation.ReserveStockInputPort;
import com.aionn.inventory.application.service.StockReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReserveStockUseCase implements ReserveStockInputPort {

    private final StockReservationService stockReservationService;

    @Override
    @Transactional
    public ReservationResult execute(ReserveStockCommand command) {
        return stockReservationService.reserve(command);
    }
}
