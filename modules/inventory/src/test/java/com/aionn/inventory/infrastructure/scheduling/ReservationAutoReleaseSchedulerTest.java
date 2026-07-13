package com.aionn.inventory.infrastructure.scheduling;

import com.aionn.inventory.application.port.out.StockReservationPersistencePort;
import com.aionn.inventory.domain.model.StockReservation;
import com.aionn.inventory.infrastructure.config.InventoryProperties;
import com.aionn.inventory.infrastructure.config.properties.InventoryReservationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationAutoReleaseSchedulerTest {

    @Mock
    private StockReservationPersistencePort reservationRepository;
    @Mock
    private ReservationAutoReleaseWorker worker;
    @Mock
    private InventoryProperties properties;
    @Mock
    private InventoryProperties.Reservation reservationConfig;
    @Mock
    private InventoryProperties.Reservation.AutoRelease autoReleaseConfig;

    private final Instant fixedInstant = Instant.parse("2026-01-01T12:00:00Z");
    private final Clock clock = Clock.fixed(fixedInstant, ZoneOffset.UTC);

    private ReservationAutoReleaseScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new ReservationAutoReleaseScheduler(reservationRepository, worker, properties, clock);
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

        when(reservationRepository.findExpired(fixedInstant, 100)).thenReturn(List.of(r1, r2));

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

        when(reservationRepository.findExpired(fixedInstant, 100)).thenReturn(List.of(r1, r2));
        doThrow(new RuntimeException("database down")).when(worker).releaseOne("RES_1");

        scheduler.releaseExpired();

        verify(worker).releaseOne("RES_1");
        verify(worker).releaseOne("RES_2");
    }
}
