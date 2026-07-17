package com.aionn.inventory.application.service;

import com.aionn.inventory.application.dto.transfer.command.CancelTransferCommand;
import com.aionn.inventory.application.dto.transfer.command.CompleteTransferCommand;
import com.aionn.inventory.application.dto.transfer.command.InitiateTransferCommand;
import com.aionn.inventory.application.mapper.InventoryResultMapper;
import com.aionn.inventory.application.port.out.InventoryItemPersistencePort;
import com.aionn.inventory.application.port.out.StockAdjustmentPersistencePort;
import com.aionn.inventory.application.port.out.StockTransferPersistencePort;
import com.aionn.inventory.application.port.out.WarehousePersistencePort;
import com.aionn.inventory.domain.exception.InventoryErrorCode;
import com.aionn.inventory.domain.exception.InventoryException;
import com.aionn.inventory.domain.model.InventoryItem;
import com.aionn.inventory.domain.model.StockTransfer;
import com.aionn.inventory.domain.model.Warehouse;
import com.aionn.inventory.domain.valueobject.InventoryItemKey;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockTransferServiceTest {

    @Mock
    WarehousePersistencePort warehouseRepository;
    @Mock
    InventoryItemPersistencePort itemRepository;
    @Mock
    StockTransferPersistencePort transferRepository;
    @Mock
    StockAdjustmentPersistencePort adjustmentRepository;
    @Mock
    InventoryResultMapper mapper;
    @Mock
    EventPublisher eventPublisher;
    @Mock
    MerchantQueryPort merchantQueryPort;
    @Mock
    Clock clock;

    @InjectMocks
    StockTransferService service;

    private static final Instant FIXED_NOW = Instant.parse("2026-01-01T00:00:00Z");

    @BeforeEach
    void stubClock() {
        org.mockito.Mockito.lenient().when(clock.instant()).thenReturn(FIXED_NOW);
    }

    @Test
    void initiateRejectsWhenSourceAndDestBelongToDifferentMerchants() {
        Warehouse from = Warehouse.create("WH_FROM", "M_1", "addr-from", 1);
        Warehouse to = Warehouse.create("WH_TO", "M_2", "addr-to", 1);
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("M_1"));
        when(warehouseRepository.findById("WH_FROM")).thenReturn(Optional.of(from));
        when(warehouseRepository.findById("WH_TO")).thenReturn(Optional.of(to));

        assertThatThrownBy(() -> service.initiate(new InitiateTransferCommand(
                "owner-1", "WH_FROM", "WH_TO", "SKU_1", 5)))
                .isInstanceOf(InventoryException.class)
                .extracting("errorCode")
                .isEqualTo(InventoryErrorCode.STOCK_TRANSFER_DIFFERENT_MERCHANT.getCode());

        verify(transferRepository, never()).save(any());
    }

    @Test
    void initiateRejectsWhenOwnerHasNoMerchant() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.initiate(new InitiateTransferCommand(
                "owner-1", "WH_FROM", "WH_TO", "SKU_1", 5)))
                .isInstanceOf(InventoryException.class)
                .extracting("errorCode")
                .isEqualTo(InventoryErrorCode.WAREHOUSE_FORBIDDEN.getCode());
    }

    @Test
    void initiateDecrementsSourceStockAndPersistsTransfer() {
        Warehouse from = Warehouse.create("WH_FROM", "M_1", "addr-from", 1);
        Warehouse to = Warehouse.create("WH_TO", "M_1", "addr-to", 1);
        InventoryItem source = InventoryItem.initialize(new InventoryItemKey("SKU_1", "WH_FROM"), 10);
        source.pullEvents();
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("M_1"));
        when(warehouseRepository.findById("WH_FROM")).thenReturn(Optional.of(from));
        when(warehouseRepository.findById("WH_TO")).thenReturn(Optional.of(to));
        when(itemRepository.lockByKey(new InventoryItemKey("SKU_1", "WH_FROM")))
                .thenReturn(Optional.of(source));
        when(itemRepository.save(any(InventoryItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transferRepository.save(any(StockTransfer.class))).thenAnswer(inv -> inv.getArgument(0));

        service.initiate(new InitiateTransferCommand("owner-1", "WH_FROM", "WH_TO", "SKU_1", 5));

        assertThat(source.getPhysicalQty()).isEqualTo(5);
        verify(transferRepository).save(any(StockTransfer.class));
    }

    @Test
    void completeRejectsWhenTransferBelongsToDifferentMerchant() {
        StockTransfer transfer = StockTransfer.initiate(
                "T_1", "M_1", "WH_FROM", "WH_TO", "SKU_1", 5);
        transfer.pullEvents();
        when(merchantQueryPort.findMerchantIdByOwnerId("attacker")).thenReturn(Optional.of("M_attacker"));
        when(transferRepository.findById("T_1")).thenReturn(Optional.of(transfer));

        assertThatThrownBy(() -> service.complete(new CompleteTransferCommand("attacker", "T_1", 5)))
                .isInstanceOf(InventoryException.class)
                .extracting("errorCode")
                .isEqualTo(InventoryErrorCode.STOCK_TRANSFER_DIFFERENT_MERCHANT.getCode());
    }

    @Test
    void cancelRefundsSourceStock() {
        StockTransfer transfer = StockTransfer.initiate(
                "T_1", "M_1", "WH_FROM", "WH_TO", "SKU_1", 5);
        transfer.pullEvents();
        InventoryItem source = InventoryItem.initialize(new InventoryItemKey("SKU_1", "WH_FROM"), 5);
        source.pullEvents();
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("M_1"));
        when(transferRepository.findById("T_1")).thenReturn(Optional.of(transfer));
        when(itemRepository.lockByKey(new InventoryItemKey("SKU_1", "WH_FROM")))
                .thenReturn(Optional.of(source));
        when(itemRepository.save(any(InventoryItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transferRepository.save(any(StockTransfer.class))).thenAnswer(inv -> inv.getArgument(0));

        service.cancel(new CancelTransferCommand("owner-1", "T_1", "damage"));

        assertThat(source.getPhysicalQty()).isEqualTo(10);
    }
}
