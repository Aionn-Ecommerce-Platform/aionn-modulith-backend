package com.aionn.inventory.application.usecase.reservation;

import com.aionn.inventory.application.dto.reservation.result.ReservationResult;
import com.aionn.inventory.application.port.in.reservation.GetReservationInputPort;
import com.aionn.inventory.application.service.StockReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetReservationUseCase implements GetReservationInputPort {

    private final StockReservationService stockReservationService;

    @Override
    @Transactional(readOnly = true)
    public ReservationResult execute(String reservationId) {
        return stockReservationService.get(reservationId);
    }
}
