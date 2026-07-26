package com.aionn.inventory.application.usecase.reservation;

import com.aionn.inventory.application.dto.reservation.command.ReleaseReservationCommand;
import com.aionn.inventory.application.dto.reservation.result.ReservationResult;
import com.aionn.inventory.application.mapper.ReservationResultMapper;
import com.aionn.inventory.application.port.in.reservation.ReleaseReservationInputPort;
import com.aionn.inventory.application.service.StockReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReleaseReservationUseCase implements ReleaseReservationInputPort {

    private final StockReservationService stockReservationService;
    private final ReservationResultMapper reservationResultMapper;

    @Override
    @Transactional
    public ReservationResult execute(ReleaseReservationCommand command) {
        return reservationResultMapper.toResult(stockReservationService.release(command));
    }
}
