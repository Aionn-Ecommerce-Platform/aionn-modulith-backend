package com.aionn.inventory.application.service;

import com.aionn.inventory.application.dto.inventory.command.ConfigureSafetyStockCommand;
import com.aionn.inventory.application.dto.inventory.command.EmergencyLockCommand;
import com.aionn.inventory.application.dto.inventory.command.InitializeStockCommand;
import com.aionn.inventory.application.dto.inventory.command.ManualAdjustmentCommand;
import com.aionn.inventory.application.mapper.InventoryResultMapper;
import com.aionn.inventory.application.port.out.InventoryItemPersistencePort;
import com.aionn.inventory.application.port.out.SafetyStockNotifier;
import com.aionn.inventory.application.port.out.StockAdjustmentPersistencePort;
import com.aionn.inventory.application.port.out.WarehousePersistencePort;
import com.aionn.inventory.domain.exception.InventoryErrorCode;
import com.aionn.inventory.domain.exception.InventoryException;
import com.aionn.inventory.domain.model.InventoryItem;
import com.aionn.inventory.domain.model.Warehouse;
import com.aionn.inventory.domain.valueobject.AdjustmentType;
import com.aionn.inventory.domain.valueobject.InventoryItemKey;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryItemServiceTest {

        @Mock
        InventoryItemPersistencePort itemRepository;
        @Mock
        WarehousePersistencePort warehouseRepository;
        @Mock
        StockAdjustmentPersistencePort adjustmentRepository;
        @Mock
        InventoryResultMapper mapper;
        @Mock
        EventPublisher eventPublisher;
        @Mock
        SafetyStockNotifier safetyStockNotifier;
        @Mock
        MerchantQueryPort merchantQueryPort;

        @Spy
        private Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC);

        @InjectMocks
        InventoryItemService service;

        @Test
        void initializeRejectsWhenOwnerHasNoMerchant() {
                when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.initialize(new InitializeStockCommand(
                                "owner-1", "SKU_1", "WH_1", 10)))
                                .isInstanceOf(InventoryException.class)
                                .extracting("errorCode")
                                .isEqualTo(InventoryErrorCode.WAREHOUSE_FORBIDDEN.getCode());

                verify(itemRepository, never()).save(any());
        }

        @Test
        void initializeRejectsWhenInventoryAlreadyExists() {
                Warehouse warehouse = Warehouse.create("WH_1", "M_1", "addr", 1);
                InventoryItem existing = InventoryItem.initialize(new InventoryItemKey("SKU_1", "WH_1"), 5);
                when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("M_1"));
                when(warehouseRepository.findById("WH_1")).thenReturn(Optional.of(warehouse));
                when(itemRepository.findByKey(new InventoryItemKey("SKU_1", "WH_1")))
                                .thenReturn(Optional.of(existing));

                assertThatThrownBy(() -> service.initialize(new InitializeStockCommand(
                                "owner-1", "SKU_1", "WH_1", 10)))
                                .isInstanceOf(InventoryException.class)
                                .extracting("errorCode")
                                .isEqualTo(InventoryErrorCode.INVENTORY_ALREADY_INITIALIZED.getCode());

                verify(itemRepository, never()).save(any());
        }

        @Test
        void initializeSavesItemAndPublishesEvents() {
                Warehouse warehouse = Warehouse.create("WH_1", "M_1", "addr", 1);
                when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("M_1"));
                when(warehouseRepository.findById("WH_1")).thenReturn(Optional.of(warehouse));
                when(itemRepository.findByKey(new InventoryItemKey("SKU_1", "WH_1")))
                                .thenReturn(Optional.empty());
                when(itemRepository.save(any(InventoryItem.class))).thenAnswer(inv -> inv.getArgument(0));

                service.initialize(new InitializeStockCommand("owner-1", "SKU_1", "WH_1", 10));

                verify(itemRepository).save(any(InventoryItem.class));
                verify(eventPublisher).publish(anyCollection());
        }

        @Test
        void manualAdjustmentRejectsOutboundType() {
                Warehouse warehouse = Warehouse.create("WH_1", "M_1", "addr", 1);
                InventoryItem item = InventoryItem.initialize(new InventoryItemKey("SKU_1", "WH_1"), 10);
                when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("M_1"));
                when(warehouseRepository.findById("WH_1")).thenReturn(Optional.of(warehouse));
                when(itemRepository.lockByKey(new InventoryItemKey("SKU_1", "WH_1")))
                                .thenReturn(Optional.of(item));

                assertThatThrownBy(() -> service.manualAdjustment(new ManualAdjustmentCommand(
                                "owner-1", "SKU_1", "WH_1", 5, AdjustmentType.OUTBOUND, "x")))
                                .isInstanceOf(InventoryException.class)
                                .extracting("errorCode")
                                .isEqualTo(InventoryErrorCode.STOCK_ADJUSTMENT_INVALID.getCode());
        }

        @Test
        void emergencyLockMarksItemLockedAndPublishesEvents() {
                InventoryItem item = InventoryItem.initialize(new InventoryItemKey("SKU_1", "WH_1"), 10);
                item.pullEvents();
                when(itemRepository.lockByKey(new InventoryItemKey("SKU_1", "WH_1")))
                                .thenReturn(Optional.of(item));
                when(itemRepository.save(any(InventoryItem.class))).thenAnswer(inv -> inv.getArgument(0));

                service.emergencyLock(new EmergencyLockCommand(
                                "admin-1", "SKU_1", "WH_1", "audit"));

                verify(itemRepository).save(item);
                verify(eventPublisher).publish(anyCollection());
        }

        @Test
        void configureSafetyStockNotifiesBreachWhenAvailableUnderThreshold() {
                Warehouse warehouse = Warehouse.create("WH_1", "M_1", "addr", 1);
                InventoryItem item = InventoryItem.initialize(new InventoryItemKey("SKU_1", "WH_1"), 5);
                item.pullEvents();
                when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("M_1"));
                when(warehouseRepository.findById("WH_1")).thenReturn(Optional.of(warehouse));
                when(itemRepository.lockByKey(new InventoryItemKey("SKU_1", "WH_1")))
                                .thenReturn(Optional.of(item));
                when(itemRepository.save(any(InventoryItem.class))).thenAnswer(inv -> inv.getArgument(0));

                service.configureSafetyStock(new ConfigureSafetyStockCommand(
                                "owner-1", "SKU_1", "WH_1", 10));

                verify(safetyStockNotifier).notifySafetyStockBreach(
                                "M_1", "SKU_1", "WH_1", 5, 10);
        }
}
