package com.aionn.inventory.application.service;

import com.aionn.inventory.application.dto.inventory.command.AuditInventoryCommand;
import com.aionn.inventory.application.dto.inventory.command.ConfigureSafetyStockCommand;
import com.aionn.inventory.application.dto.inventory.command.EmergencyLockCommand;
import com.aionn.inventory.application.dto.inventory.command.EmergencyUnlockCommand;
import com.aionn.inventory.application.dto.inventory.command.InitializeStockCommand;
import com.aionn.inventory.application.dto.inventory.command.ManualAdjustmentCommand;
import com.aionn.inventory.application.dto.inventory.command.TrackBatchAndExpiryCommand;
import com.aionn.inventory.application.dto.inventory.result.InventoryItemResult;
import com.aionn.inventory.application.mapper.InventoryResultMapper;
import com.aionn.inventory.application.port.out.InventoryItemPersistencePort;
import com.aionn.inventory.application.port.out.SafetyStockNotifier;
import com.aionn.inventory.application.port.out.StockAdjustmentPersistencePort;
import com.aionn.inventory.application.port.out.WarehousePersistencePort;
import com.aionn.inventory.domain.event.InventoryItemEvents;
import com.aionn.inventory.domain.exception.InventoryErrorCode;
import com.aionn.inventory.domain.exception.InventoryException;
import com.aionn.inventory.domain.model.InventoryItem;
import com.aionn.inventory.domain.model.StockAdjustment;
import com.aionn.inventory.domain.model.Warehouse;
import com.aionn.inventory.domain.valueobject.InventoryItemKey;
import com.aionn.inventory.application.dto.common.PageResult;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.domain.model.EventEnvelope;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InventoryItemService {

    private final InventoryItemPersistencePort itemRepository;
    private final WarehousePersistencePort warehouseRepository;
    private final StockAdjustmentPersistencePort adjustmentRepository;
    private final InventoryResultMapper mapper;
    private final EventPublisher eventPublisher;
    private final SafetyStockNotifier safetyStockNotifier;
    private final MerchantQueryPort merchantQueryPort;
    private final Clock clock;

    public InventoryItemResult initialize(InitializeStockCommand command) {
        OwnerContext ctx = ownerContext(command.ownerId(), command.warehouseId());
        if (!ctx.warehouse().getStatus().canFulfill()) {
            throw new InventoryException(InventoryErrorCode.WAREHOUSE_INVALID_TRANSITION,
                    "Cannot initialize stock for a non-active warehouse");
        }
        InventoryItemKey key = new InventoryItemKey(command.skuId(), command.warehouseId());
        if (itemRepository.findByKey(key).isPresent()) {
            throw new InventoryException(InventoryErrorCode.INVENTORY_ALREADY_INITIALIZED);
        }
        InventoryItem item = InventoryItem.initialize(key, command.initialQty(), clock);
        InventoryItem saved;
        try {
            saved = itemRepository.save(item);
        } catch (DataIntegrityViolationException ex) {
            throw new InventoryException(InventoryErrorCode.INVENTORY_ALREADY_INITIALIZED);
        }
        eventPublisher.publish(item.pullEvents());
        return mapper.toResult(saved);
    }

    public InventoryItemResult configureSafetyStock(ConfigureSafetyStockCommand command) {
        OwnerContext ctx = ownerContext(command.ownerId(), command.warehouseId());
        InventoryItem item = lockedItem(command.skuId(), command.warehouseId());
        item.configureSafetyStock(command.safetyStockQty(), clock);
        InventoryItem saved = itemRepository.save(item);
        publishWithSafetyHook(item.pullEvents(), ctx.merchantId());
        return mapper.toResult(saved);
    }

    public InventoryItemResult trackBatchAndExpiry(TrackBatchAndExpiryCommand command) {
        ownerContext(command.ownerId(), command.warehouseId());
        InventoryItem item = lockedItem(command.skuId(), command.warehouseId());
        item.trackBatchAndExpiry(command.batchNo(), command.expiryDate(), clock);
        InventoryItem saved = itemRepository.save(item);
        eventPublisher.publish(item.pullEvents());
        return mapper.toResult(saved);
    }

    public InventoryItemResult manualAdjustment(ManualAdjustmentCommand command) {
        OwnerContext ctx = ownerContext(command.ownerId(), command.warehouseId());
        InventoryItem item = lockedItem(command.skuId(), command.warehouseId());

        int signedDelta = switch (command.type()) {
            case MANUAL_INCREASE, TRANSFER_IN -> Math.abs(command.qty());
            case MANUAL_DECREASE, DAMAGED, TRANSFER_OUT -> -Math.abs(command.qty());
            case OUTBOUND -> throw new InventoryException(InventoryErrorCode.STOCK_ADJUSTMENT_INVALID,
                    "Use commit reservation for OUTBOUND");
        };
        item.adjust(signedDelta, command.type(), command.reason(), clock);
        item.emitBreachIfApplicable(clock);
        InventoryItem saved = itemRepository.save(item);

        StockAdjustment adjustment = StockAdjustment.manual(IdGenerator.ulid(),
                command.skuId(), command.warehouseId(), Math.abs(signedDelta), command.type(), command.reason(), clock);
        adjustmentRepository.save(adjustment);
        eventPublisher.publish(adjustment.pullEvents());

        publishWithSafetyHook(item.pullEvents(), ctx.merchantId());
        return mapper.toResult(saved);
    }

    public InventoryItemResult emergencyLock(EmergencyLockCommand command) {
        InventoryItem item = lockedItem(command.skuId(), command.warehouseId());
        item.emergencyLock(command.adminId(), command.reason(), clock);
        InventoryItem saved = itemRepository.save(item);
        eventPublisher.publish(item.pullEvents());
        return mapper.toResult(saved);
    }

    public InventoryItemResult emergencyUnlock(EmergencyUnlockCommand command) {
        InventoryItem item = lockedItem(command.skuId(), command.warehouseId());
        item.emergencyUnlock(command.adminId(), clock);
        InventoryItem saved = itemRepository.save(item);
        eventPublisher.publish(item.pullEvents());
        return mapper.toResult(saved);
    }

    public InventoryItemResult auditInventory(AuditInventoryCommand command) {
        OwnerContext ctx = ownerContext(command.ownerId(), command.warehouseId());
        InventoryItem item = lockedItem(command.skuId(), command.warehouseId());
        item.recordAudit(IdGenerator.ulid(), command.actualQty(), clock);
        item.emitBreachIfApplicable(clock);
        InventoryItem saved = itemRepository.save(item);
        publishWithSafetyHook(item.pullEvents(), ctx.merchantId());
        return mapper.toResult(saved);
    }

    @Transactional(readOnly = true)
    public InventoryItemResult get(String skuId, String warehouseId) {
        return itemRepository.findByKey(new InventoryItemKey(skuId, warehouseId))
                .map(mapper::toResult)
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.INVENTORY_ITEM_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<InventoryItemResult> listBySku(String skuId) {
        return itemRepository.findBySku(skuId).stream()
                .map(mapper::toResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<InventoryItemResult> listByWarehouse(String ownerId, String warehouseId, Pageable pageable) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.WAREHOUSE_NOT_FOUND));
        String merchantId = merchantQueryPort.findMerchantIdByOwnerId(ownerId)
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.WAREHOUSE_FORBIDDEN,
                        "No merchant registered for the authenticated user"));
        warehouse.ensureOwnedBy(merchantId);
        Page<InventoryItemResult> page = itemRepository.findByWarehouse(warehouseId, pageable).map(mapper::toResult);
        return new PageResult<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getNumberOfElements(),
                page.getTotalElements());
    }

    private OwnerContext ownerContext(String ownerId, String warehouseId) {
        String merchantId = merchantQueryPort.findMerchantIdByOwnerId(ownerId)
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.WAREHOUSE_FORBIDDEN,
                        "No merchant registered for the authenticated user"));
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.WAREHOUSE_NOT_FOUND));
        warehouse.ensureOwnedBy(merchantId);
        return new OwnerContext(merchantId, warehouse);
    }

    private InventoryItem lockedItem(String skuId, String warehouseId) {
        return itemRepository.lockByKey(new InventoryItemKey(skuId, warehouseId))
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.INVENTORY_ITEM_NOT_FOUND));
    }

    private void publishWithSafetyHook(List<EventEnvelope> events, String merchantId) {
        eventPublisher.publish(events);
        for (EventEnvelope envelope : events) {
            if (envelope.payload() instanceof InventoryItemEvents.SafetyStockBreached breach) {
                safetyStockNotifier.notifySafetyStockBreach(merchantId,
                        breach.skuId(), breach.warehouseId(), breach.availableQty(), breach.safetyStockQty());
            }
        }
    }

    private record OwnerContext(String merchantId, Warehouse warehouse) {
    }
}
