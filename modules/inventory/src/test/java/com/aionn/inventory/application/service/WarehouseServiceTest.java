package com.aionn.inventory.application.service;

import com.aionn.inventory.application.dto.warehouse.command.*;
import com.aionn.inventory.application.port.out.WarehousePersistencePort;
import com.aionn.inventory.domain.exception.InventoryErrorCode;
import com.aionn.inventory.domain.exception.InventoryException;
import com.aionn.inventory.domain.model.Warehouse;
import com.aionn.inventory.domain.valueobject.WarehouseStatus;
import com.aionn.sharedkernel.application.port.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

    @Mock
    WarehousePersistencePort warehouseRepository;
    @Mock
    EventPublisher eventPublisher;
    @Mock
    Clock clock;

    @InjectMocks
    WarehouseService warehouseService;

    private static final Instant FIXED_NOW = Instant.parse("2026-01-01T00:00:00Z");

    @BeforeEach
    void stubClock() {
        org.mockito.Mockito.lenient().when(clock.instant()).thenReturn(FIXED_NOW);
    }

    @Test
    @DisplayName("create() uses the merchant id verified by the web adapter")
    void create_usesVerifiedMerchantId() {
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(inv -> inv.getArgument(0));

        warehouseService.create(new CreateWarehouseCommand("M_1", "addr", 1));

        ArgumentCaptor<Warehouse> captor = ArgumentCaptor.forClass(Warehouse.class);
        verify(warehouseRepository).save(captor.capture());
        assertEquals("M_1", captor.getValue().getMerchantId());
    }

    @Test
    @DisplayName("changeStatus() rejects an attacker acting on another merchant's warehouse")
    void changeStatus_rejectsForeignWarehouse() {
        Warehouse victim = Warehouse.create("W_1", "M_victim", "addr", 1);
        when(warehouseRepository.findById("W_1")).thenReturn(Optional.of(victim));

        InventoryException ex = assertThrows(InventoryException.class,
                () -> warehouseService.changeStatus(new ChangeStatusCommand("W_1", "M_attacker", "ACTIVE")));

        assertEquals(InventoryErrorCode.WAREHOUSE_FORBIDDEN.getCode(), ex.getErrorCode());
        verify(warehouseRepository, never()).save(any());
    }

    @Test
    @DisplayName("changeStatus() throws INVALID_ARGUMENT when status value is unknown")
    void changeStatus_throwsInvalidStatus() {
        Warehouse warehouse = Warehouse.create("W_1", "M_1", "addr", 1);
        when(warehouseRepository.findById("W_1")).thenReturn(Optional.of(warehouse));

        InventoryException ex = assertThrows(InventoryException.class,
                () -> warehouseService.changeStatus(new ChangeStatusCommand("W_1", "M_1", "UNKNOWN_STATUS")));

        assertEquals(InventoryErrorCode.INVALID_ARGUMENT.getCode(), ex.getErrorCode());
    }

    @Test
    @DisplayName("adjustPriority() changes warehouse priority and saves successfully")
    void adjustPriority_modifiesPriorityAndSaves() {
        Warehouse warehouse = Warehouse.create("W_1", "M_1", "addr", 1);
        when(warehouseRepository.findById("W_1")).thenReturn(Optional.of(warehouse));
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(inv -> inv.getArgument(0));

        warehouseService.adjustPriority(new AdjustPriorityCommand("W_1", "M_1", 5));

        verify(warehouseRepository).save(warehouse);
        verify(eventPublisher).publish(anyCollection());
    }

    @Test
    @DisplayName("suspend() changes status to SUSPENDED by admin")
    void suspend_updatesWarehouseStatus() {
        Warehouse warehouse = Warehouse.create("W_1", "M_1", "addr", 1);
        when(warehouseRepository.findById("W_1")).thenReturn(Optional.of(warehouse));
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(inv -> inv.getArgument(0));

        warehouseService.suspend(new SuspendWarehouseCommand("W_1", "admin-1", "Suspended reason"));

        verify(warehouseRepository).save(warehouse);
        assertEquals(WarehouseStatus.SUSPENDED, warehouse.getStatus());
    }

    @Test
    @DisplayName("liftSuspension() reactivates warehouse")
    void liftSuspension_reactivatesWarehouse() {
        Warehouse warehouse = Warehouse.create("W_1", "M_1", "addr", 1);
        warehouse.suspend("admin-1", "Suspended reason", clock);
        when(warehouseRepository.findById("W_1")).thenReturn(Optional.of(warehouse));
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(inv -> inv.getArgument(0));

        warehouseService.liftSuspension(new LiftSuspensionCommand("W_1", "admin-1"));

        verify(warehouseRepository).save(warehouse);
        assertEquals(WarehouseStatus.ACTIVE, warehouse.getStatus());
    }

    @Test
    @DisplayName("get() returns warehouse result when found")
    void get_returnsWarehouseResult() {
        Warehouse warehouse = Warehouse.create("W_1", "M_1", "addr", 1);
        when(warehouseRepository.findById("W_1")).thenReturn(Optional.of(warehouse));

        Warehouse result = warehouseService.get("W_1");

        assertEquals(warehouse, result);
    }

    @Test
    @DisplayName("get() throws WAREHOUSE_NOT_FOUND when warehouse is missing")
    void get_throwsNotFound() {
        when(warehouseRepository.findById("W_1")).thenReturn(Optional.empty());

        assertThrows(InventoryException.class, () -> warehouseService.get("W_1"));
    }

    @Test
    void getOwned_rejectsForeignWarehouse() {
        Warehouse warehouse = Warehouse.create("W_1", "M_1", "addr", 1);
        when(warehouseRepository.findById("W_1")).thenReturn(Optional.of(warehouse));

        InventoryException ex = assertThrows(InventoryException.class,
                () -> warehouseService.getOwned("W_1", "M_2"));

        assertEquals(InventoryErrorCode.WAREHOUSE_FORBIDDEN.getCode(), ex.getErrorCode());
    }

    @Test
    @DisplayName("listByOwner() returns all warehouses sorted by priority")
    void listByOwner_returnsSortedList() {
        Warehouse w1 = Warehouse.create("W_1", "M_1", "addr1", 1);
        Warehouse w2 = Warehouse.create("W_2", "M_1", "addr2", 2);
        when(warehouseRepository.findByMerchantOrderByPriority("M_1")).thenReturn(java.util.List.of(w1, w2));

        warehouseService.listByOwner("M_1");

        verify(warehouseRepository).findByMerchantOrderByPriority("M_1");
    }
}
