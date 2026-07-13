package com.aionn.inventory.application.service;

import com.aionn.inventory.application.dto.reservation.command.CommitReservationCommand;
import com.aionn.inventory.application.dto.reservation.command.ReleaseReservationCommand;
import com.aionn.inventory.application.dto.reservation.command.ReserveStockCommand;
import com.aionn.inventory.application.mapper.InventoryResultMapper;
import com.aionn.inventory.application.port.out.InventoryItemPersistencePort;
import com.aionn.inventory.application.port.out.OutboundOrderNotifier;
import com.aionn.inventory.application.port.out.StockAdjustmentPersistencePort;
import com.aionn.inventory.application.port.out.StockReservationPersistencePort;
import com.aionn.inventory.domain.exception.InventoryErrorCode;
import com.aionn.inventory.domain.exception.InventoryException;
import com.aionn.inventory.domain.model.InventoryItem;
import com.aionn.inventory.domain.model.StockAdjustment;
import com.aionn.inventory.domain.model.StockReservation;
import com.aionn.inventory.domain.valueobject.InventoryItemKey;
import com.aionn.sharedkernel.application.port.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockReservationServiceTest {

    @Mock
    InventoryItemPersistencePort itemRepository;
    @Mock
    StockReservationPersistencePort reservationRepository;
    @Mock
    StockAdjustmentPersistencePort adjustmentRepository;
    @Mock
    InventoryResultMapper mapper;
    @Mock
    EventPublisher eventPublisher;
    @Mock
    OutboundOrderNotifier outboundOrderNotifier;
    @Mock
    Clock clock;

    @InjectMocks
    StockReservationService service;

    private static final Instant FIXED_NOW = Instant.parse("2026-01-01T00:00:00Z");

    @BeforeEach
    void stubClock() {
        org.mockito.Mockito.lenient().when(clock.instant()).thenReturn(FIXED_NOW);
    }

    private static final InventoryItemKey KEY = new InventoryItemKey("SKU_1", "WH_1");

    @Test
    void reserveSucceedsWhenAvailableStockSufficient() {
        InventoryItem item = InventoryItem.initialize(KEY, 10);
        item.pullEvents();
        when(itemRepository.lockByKey(KEY)).thenReturn(Optional.of(item));
        when(itemRepository.save(any(InventoryItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reservationRepository.save(any(StockReservation.class))).thenAnswer(inv -> inv.getArgument(0));

        service.reserve(new ReserveStockCommand("SKU_1", "WH_1", "ORDER_1", 5, 60));

        ArgumentCaptor<StockReservation> captor = ArgumentCaptor.forClass(StockReservation.class);
        verify(reservationRepository).save(captor.capture());
        assertThat(captor.getValue().getQty()).isEqualTo(5);
        assertThat(item.getAvailableQty()).isEqualTo(5);
    }

    @Test
    void reservePersistsFailedReservationWhenStockInsufficient() {
        InventoryItem item = InventoryItem.initialize(KEY, 1);
        item.pullEvents();
        when(itemRepository.lockByKey(KEY)).thenReturn(Optional.of(item));
        when(reservationRepository.save(any(StockReservation.class))).thenAnswer(inv -> inv.getArgument(0));

        service.reserve(new ReserveStockCommand("SKU_1", "WH_1", "ORDER_1", 5, 60));

        verify(reservationRepository).save(any(StockReservation.class));
        verify(outboundOrderNotifier).notifyReservationFailed(
                eq("ORDER_1"), eq("SKU_1"), eq("WH_1"), eq(5), anyString());
        verify(itemRepository, never()).save(any());
    }

    @Test
    void reserveThrowsWhenItemNotFound() {
        when(itemRepository.lockByKey(KEY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reserve(
                new ReserveStockCommand("SKU_1", "WH_1", "ORDER_1", 1, 60)))
                .isInstanceOf(InventoryException.class)
                .extracting("errorCode")
                .isEqualTo(InventoryErrorCode.INVENTORY_ITEM_NOT_FOUND.getCode());
    }

    @Test
    void commitDecrementsPhysicalAndPersistsAdjustment() {
        InventoryItem item = InventoryItem.initialize(KEY, 10);
        item.reserve(3);
        item.pullEvents();
        Instant exp = Instant.now().plus(Duration.ofMinutes(5));
        StockReservation reservation = StockReservation.reserve("R_1", "SKU_1", "WH_1", "ORDER_1", 3, exp);
        reservation.pullEvents();

        when(reservationRepository.findById("R_1")).thenReturn(Optional.of(reservation));
        when(itemRepository.lockByKey(KEY)).thenReturn(Optional.of(item));
        when(itemRepository.save(any(InventoryItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reservationRepository.save(any(StockReservation.class))).thenAnswer(inv -> inv.getArgument(0));

        service.commit(new CommitReservationCommand("R_1"));

        assertThat(item.getPhysicalQty()).isEqualTo(7);
        verify(adjustmentRepository).save(any(StockAdjustment.class));
        verify(outboundOrderNotifier).notifyOutbound(
                "ORDER_1", "R_1", "SKU_1", "WH_1", 3);
    }

    @Test
    void commitRejectsWhenReservationNotInReservedState() {
        Instant exp = Instant.now().plus(Duration.ofMinutes(5));
        StockReservation reservation = StockReservation.reserve("R_1", "SKU_1", "WH_1", "ORDER_1", 3, exp);
        reservation.commit();
        reservation.pullEvents();
        when(reservationRepository.findById("R_1")).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> service.commit(new CommitReservationCommand("R_1")))
                .isInstanceOf(InventoryException.class)
                .extracting("errorCode")
                .isEqualTo(InventoryErrorCode.STOCK_RESERVATION_INVALID_STATE.getCode());
    }

    @Test
    void releaseRestoresAvailableAndPublishesEvents() {
        InventoryItem item = InventoryItem.initialize(KEY, 10);
        item.reserve(4);
        item.pullEvents();
        Instant exp = Instant.now().plus(Duration.ofMinutes(5));
        StockReservation reservation = StockReservation.reserve("R_1", "SKU_1", "WH_1", "ORDER_1", 4, exp);
        reservation.pullEvents();

        when(reservationRepository.findById("R_1")).thenReturn(Optional.of(reservation));
        when(itemRepository.lockByKey(KEY)).thenReturn(Optional.of(item));
        when(itemRepository.save(any(InventoryItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reservationRepository.save(any(StockReservation.class))).thenAnswer(inv -> inv.getArgument(0));

        service.release(new ReleaseReservationCommand("R_1", "cancelled"));

        assertThat(item.getAvailableQty()).isEqualTo(10);
        verify(eventPublisher, org.mockito.Mockito.atLeastOnce()).publish(anyCollection());
    }
}
