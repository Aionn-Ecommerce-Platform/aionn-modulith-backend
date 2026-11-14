package com.aionn.inventory.infrastructure.scheduling;

import com.aionn.inventory.application.port.out.StockReservationPersistencePort;
import com.aionn.inventory.domain.model.StockReservation;
import com.aionn.inventory.infrastructure.config.InventoryProperties;
import com.aionn.contracts.port.ordering.OrderQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationAutoReleaseSchedulerTest {

    @Mock
    private StockReservationPersistencePort stockReservationPersistencePort;
    @Mock
    private ReservationAutoReleaseWorker worker;
    @Mock
    private InventoryProperties properties;
    @Mock
    private InventoryProperties.Reservation reservationConfig;
    @Mock
    private InventoryProperties.Reservation.AutoRelease autoReleaseConfig;
    @Mock
    private com.aionn.contracts.port.ordering.OrderQueryPort orderQueryPort;

    private final Instant fixedInstant = Instant.parse("2026-01-01T12:00:00Z");
    private final Clock clock = Clock.fixed(fixedInstant, ZoneOffset.UTC);

    private ReservationAutoReleaseScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new ReservationAutoReleaseScheduler(
                stockReservationPersistencePort, worker, properties, orderQueryPort, clock);
        when(properties.reservation()).thenReturn(reservationConfig);
        when(reservationConfig.autoRelease()).thenReturn(autoReleaseConfig);
        when(autoReleaseConfig.batchSize()).thenReturn(100);
    }

    @Test
    void releaseExpiredProcessesAllExpiredReservations() {
        StockReservation r1 = mock(StockReservation.class);
        StockReservation r2 = mock(StockReservation.class);
        when(r1.getReservationId()).thenReturn("RES_1");
        when(r2.getReservationId()).thenReturn("RES_2");

        when(stockReservationPersistencePort.findExpired(fixedInstant, 100)).thenReturn(List.of(r1, r2));

        scheduler.releaseExpired();

        verify(worker).releaseOne("RES_1");
        verify(worker).releaseOne("RES_2");
    }

    @Test
    void releaseExpiredContinuesWhenWorkerThrows() {
        StockReservation r1 = mock(StockReservation.class);
        StockReservation r2 = mock(StockReservation.class);
        when(r1.getReservationId()).thenReturn("RES_1");
        when(r2.getReservationId()).thenReturn("RES_2");

        when(stockReservationPersistencePort.findExpired(fixedInstant, 100)).thenReturn(List.of(r1, r2));
        doThrow(new RuntimeException("database down")).when(worker).releaseOne("RES_1");

        scheduler.releaseExpired();

        verify(worker).releaseOne("RES_1");
        verify(worker).releaseOne("RES_2");
    }

    @Test
    void keepsExpiredReservationWhenOrderIsStillActive() {
        StockReservation r1 = mock(StockReservation.class);
        when(r1.getReservationId()).thenReturn("RES_1");
        when(r1.getOrderId()).thenReturn("ORD_1");
        when(stockReservationPersistencePort.findExpired(fixedInstant, 100)).thenReturn(List.of(r1));
        when(orderQueryPort.findOrderSummary("ORD_1")).thenReturn(Optional.of(
                new OrderQueryPort.OrderSummary("ORD_1", "U_1", "M_1", null, "VND", "PREPARING")));

        scheduler.releaseExpired();

        verify(worker, never()).releaseOne("RES_1");
    }
}
