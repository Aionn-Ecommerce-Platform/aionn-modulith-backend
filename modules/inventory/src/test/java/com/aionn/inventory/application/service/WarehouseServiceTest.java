package com.aionn.inventory.application.service;

import com.aionn.inventory.application.dto.warehouse.command.ChangeStatusCommand;
import com.aionn.inventory.application.dto.warehouse.command.CreateWarehouseCommand;
import com.aionn.inventory.application.mapper.InventoryResultMapper;
import com.aionn.inventory.application.port.out.WarehousePersistencePort;
import com.aionn.inventory.domain.exception.InventoryErrorCode;
import com.aionn.inventory.domain.exception.InventoryException;
import com.aionn.inventory.domain.model.Warehouse;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

    @Mock
    WarehousePersistencePort warehouseRepository;
    @Mock
    InventoryResultMapper mapper;
    @Mock
    EventPublisher eventPublisher;
    @Mock
    MerchantQueryPort merchantQueryPort;
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
    @DisplayName("create() throws WAREHOUSE_FORBIDDEN when the authenticated user has no merchant")
    void create_throwsWhenOwnerHasNoMerchant() {
        when(merchantQueryPort.findMerchantIdByOwnerId("user-1")).thenReturn(Optional.empty());

        InventoryException ex = assertThrows(InventoryException.class,
                () -> warehouseService.create(new CreateWarehouseCommand("user-1", "addr", 1)));

        assertEquals(InventoryErrorCode.WAREHOUSE_FORBIDDEN.getCode(), ex.getErrorCode());
        verifyNoInteractions(warehouseRepository, eventPublisher);
    }

    @Test
    @DisplayName("create() resolves merchantId from authenticated owner instead of trusting the client")
    void create_resolvesMerchantIdFromOwner() {
        when(merchantQueryPort.findMerchantIdByOwnerId("user-1")).thenReturn(Optional.of("M_1"));
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(inv -> inv.getArgument(0));

        warehouseService.create(new CreateWarehouseCommand("user-1", "addr", 1));

        ArgumentCaptor<Warehouse> captor = ArgumentCaptor.forClass(Warehouse.class);
        verify(warehouseRepository).save(captor.capture());
        assertEquals("M_1", captor.getValue().getMerchantId());
    }

    @Test
    @DisplayName("changeStatus() rejects an attacker acting on another merchant's warehouse")
    void changeStatus_rejectsForeignWarehouse() {
        when(merchantQueryPort.findMerchantIdByOwnerId("attacker")).thenReturn(Optional.of("M_attacker"));
        Warehouse victim = Warehouse.create("W_1", "M_victim", "addr", 1);
        when(warehouseRepository.findById("W_1")).thenReturn(Optional.of(victim));

        InventoryException ex = assertThrows(InventoryException.class,
                () -> warehouseService.changeStatus(new ChangeStatusCommand("W_1", "attacker", "ACTIVE")));

        assertEquals(InventoryErrorCode.WAREHOUSE_FORBIDDEN.getCode(), ex.getErrorCode());
        verify(warehouseRepository, never()).save(any());
    }
}
