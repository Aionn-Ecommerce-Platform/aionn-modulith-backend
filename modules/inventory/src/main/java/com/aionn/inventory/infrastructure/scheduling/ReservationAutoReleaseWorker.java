package com.aionn.inventory.infrastructure.scheduling;

import com.aionn.inventory.application.dto.reservation.command.ReleaseReservationCommand;
import com.aionn.inventory.application.service.StockReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ReservationAutoReleaseWorker {

    private final StockReservationService reservationService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void releaseOne(String reservationId) {
        reservationService.release(new ReleaseReservationCommand(reservationId, "expired"));
    }
}
