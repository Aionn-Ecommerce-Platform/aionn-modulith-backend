package com.aionn.inventory.infrastructure.integration;

import com.aionn.inventory.application.dto.reservation.command.CommitReservationCommand;
import com.aionn.inventory.application.dto.reservation.command.ReleaseReservationCommand;
import com.aionn.inventory.application.dto.reservation.command.ReserveStockCommand;
import com.aionn.inventory.application.dto.reservation.result.ReservationResult;
import com.aionn.inventory.application.service.StockReservationService;
import com.aionn.sharedkernel.integration.port.inventory.InventoryStockReservationPort.Reservation;
import com.aionn.sharedkernel.integration.port.inventory.InventoryStockReservationPort.ReservationException;
import com.aionn.sharedkernel.integration.port.inventory.InventoryStockReservationPort.ReservationLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryStockReservationAdapterTest {

    @Mock
    private StockReservationService reservationService;

    private InventoryStockReservationAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new InventoryStockReservationAdapter(reservationService);
    }

    @Test
    void reserveAllSucceeds() {
        ReservationLine line1 = new ReservationLine("SKU_1", "WH_1", 2, new BigDecimal("10.0"), "USD");
        ReservationLine line2 = new ReservationLine("SKU_2", "WH_1", 3, new BigDecimal("15.0"), "USD");

        when(reservationService.reserve(any(ReserveStockCommand.class)))
                .thenReturn(new ReservationResult("RES_1", "SKU_1", "WH_1", "ORDER_1", 2, "RESERVED", Instant.now(), Instant.now(), null))
                .thenReturn(new ReservationResult("RES_2", "SKU_2", "WH_1", "ORDER_1", 3, "RESERVED", Instant.now(), Instant.now(), null));

        List<Reservation> results = adapter.reserveAll("ORDER_1", List.of(line1, line2), 300);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).reservationId()).isEqualTo("RES_1");
        assertThat(results.get(1).reservationId()).isEqualTo("RES_2");
    }

    @Test
    void reserveAllTriggersCompensationOnFailure() {
        ReservationLine line1 = new ReservationLine("SKU_1", "WH_1", 2, new BigDecimal("10.0"), "USD");
        ReservationLine line2 = new ReservationLine("SKU_2", "WH_1", 3, new BigDecimal("15.0"), "USD");

        when(reservationService.reserve(any(ReserveStockCommand.class)))
                .thenReturn(new ReservationResult("RES_1", "SKU_1", "WH_1", "ORDER_1", 2, "RESERVED", Instant.now(), Instant.now(), null))
                .thenThrow(new RuntimeException("Stock low"));

        assertThatThrownBy(() -> adapter.reserveAll("ORDER_1", List.of(line1, line2), 300))
                .isInstanceOf(ReservationException.class);

        verify(reservationService).release(any(ReleaseReservationCommand.class));
    }

    @Test
    void commitDelegatesToService() {
        adapter.commit("RES_1");
        verify(reservationService).commit(any(CommitReservationCommand.class));
    }

    @Test
    void releaseDelegatesToService() {
        adapter.release("RES_1", "cancel");
        verify(reservationService).release(any(ReleaseReservationCommand.class));
    }
}
