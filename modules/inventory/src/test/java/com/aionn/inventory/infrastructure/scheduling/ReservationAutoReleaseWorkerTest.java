package com.aionn.inventory.infrastructure.scheduling;

import com.aionn.inventory.application.dto.reservation.command.ReleaseReservationCommand;
import com.aionn.inventory.application.service.StockReservationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReservationAutoReleaseWorkerTest {

    @Mock
    private StockReservationService reservationService;

    @InjectMocks
    private ReservationAutoReleaseWorker worker;

    @Test
    void releaseOneDelegatesToStockReservationService() {
        worker.releaseOne("RES_1");

        verify(reservationService).release(any(ReleaseReservationCommand.class));
    }
}
