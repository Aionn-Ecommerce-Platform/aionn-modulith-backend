package com.aionn.inventory.application.service;

import com.aionn.inventory.application.dto.reservation.command.CommitReservationCommand;
import com.aionn.inventory.application.dto.reservation.command.ReleaseReservationCommand;
import com.aionn.inventory.application.dto.reservation.command.ReserveStockCommand;
import com.aionn.inventory.application.port.out.InventoryItemPersistencePort;
import com.aionn.inventory.application.port.out.StockAdjustmentPersistencePort;
import com.aionn.inventory.application.port.out.StockReservationPersistencePort;
import com.aionn.inventory.application.port.out.WarehousePersistencePort;
import com.aionn.inventory.application.policy.InventoryReservationPolicy;
import com.aionn.inventory.domain.exception.InventoryErrorCode;
import com.aionn.inventory.domain.exception.InventoryException;
import com.aionn.inventory.domain.model.InventoryItem;
import com.aionn.inventory.domain.model.StockAdjustment;
import com.aionn.inventory.domain.model.StockReservation;
import com.aionn.inventory.domain.valueobject.InventoryItemKey;
import com.aionn.inventory.domain.valueobject.ReservationStatus;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StockReservationService {

        private final InventoryItemPersistencePort inventoryItemPersistencePort;
        private final StockReservationPersistencePort stockReservationPersistencePort;
        private final StockAdjustmentPersistencePort stockAdjustmentPersistencePort;
        private final WarehousePersistencePort warehousePersistencePort;
        private final EventPublisher eventPublisher;
        private final InventoryReservationPolicy reservationPolicy;
        private final Clock clock;

        public StockReservation reserve(ReserveStockCommand command) {
                if (command.orderId() != null && !command.orderId().isBlank()) {
                        var existing = stockReservationPersistencePort.findActiveReservation(
                                        command.orderId(), command.skuId(), command.warehouseId());
                        if (existing.isPresent()) {
                                return existing.get();
                        }
                }

                InventoryItemKey key = new InventoryItemKey(command.skuId(), command.warehouseId());
                InventoryItem item = inventoryItemPersistencePort.lockByKey(key)
                                .orElseThrow(() -> new InventoryException(InventoryErrorCode.INVENTORY_ITEM_NOT_FOUND));

                boolean warehouseCanFulfill = warehousePersistencePort.findById(command.warehouseId())
                                .map(warehouse -> warehouse.getStatus().canFulfill())
                                .orElse(false);
                if (!warehouseCanFulfill || item.isLocked() || item.getAvailableQty() < command.qty()) {
                        String reason = !warehouseCanFulfill
                                        ? "Warehouse is not active"
                                        : item.isLocked() ? "Inventory locked" : "Insufficient stock";
                        StockReservation failed = StockReservation.failed(IdGenerator.ulid(),
                                        command.skuId(), command.warehouseId(), command.orderId(),
                                        command.qty(), reason, clock);
                        StockReservation saved = stockReservationPersistencePort.save(failed);
                        eventPublisher.publish(failed.pullEvents());
                        return saved;
                }

                item.reserve(command.qty(), clock);
                inventoryItemPersistencePort.save(item);

                Instant expiresAt = clock.instant().plus(Duration.ofSeconds(ttlSeconds(command)));
                StockReservation reservation = StockReservation.reserve(IdGenerator.ulid(),
                                command.skuId(), command.warehouseId(), command.orderId(), command.qty(), expiresAt,
                                clock);
                try {
                        StockReservation saved = stockReservationPersistencePort.save(reservation);
                        eventPublisher.publish(reservation.pullEvents());
                        eventPublisher.publish(item.pullEvents());
                        return saved;
                } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                        return stockReservationPersistencePort.findActiveReservation(
                                        command.orderId(), command.skuId(), command.warehouseId())
                                        .orElseThrow(() -> ex);
                }
        }

        public StockReservation commit(CommitReservationCommand command) {
                StockReservation reservation = stockReservationPersistencePort.lockForUpdate(command.reservationId())
                                .orElseThrow(() -> new InventoryException(
                                                InventoryErrorCode.STOCK_RESERVATION_NOT_FOUND));
                if (reservation.getStatus() == ReservationStatus.COMMITTED) {
                        return reservation;
                }
                if (reservation.getStatus() != ReservationStatus.RESERVED) {
                        throw new InventoryException(InventoryErrorCode.STOCK_RESERVATION_INVALID_STATE);
                }
                InventoryItem item = inventoryItemPersistencePort.lockByKey(
                                new InventoryItemKey(reservation.getSkuId(), reservation.getWarehouseId()))
                                .orElseThrow(() -> new InventoryException(InventoryErrorCode.INVENTORY_ITEM_NOT_FOUND));

                item.commit(reservation.getQty(), clock);
                inventoryItemPersistencePort.save(item);

                reservation.commit(clock);
                StockReservation saved = stockReservationPersistencePort.save(reservation);

                StockAdjustment outbound = StockAdjustment.outbound(IdGenerator.ulid(),
                                reservation.getSkuId(), reservation.getWarehouseId(), reservation.getQty(),
                                reservation.getOrderId(), clock);
                stockAdjustmentPersistencePort.save(outbound);

                eventPublisher.publish(reservation.pullEvents());
                eventPublisher.publish(item.pullEvents());
                eventPublisher.publish(outbound.pullEvents());
                return saved;
        }

        public StockReservation release(ReleaseReservationCommand command) {
                StockReservation reservation = stockReservationPersistencePort.lockForUpdate(command.reservationId())
                                .orElseThrow(() -> new InventoryException(
                                                InventoryErrorCode.STOCK_RESERVATION_NOT_FOUND));
                if (reservation.getStatus() == ReservationStatus.RELEASED) {
                        return reservation;
                }
                if (reservation.getStatus() != ReservationStatus.RESERVED) {
                        throw new InventoryException(InventoryErrorCode.STOCK_RESERVATION_INVALID_STATE);
                }
                InventoryItem item = inventoryItemPersistencePort.lockByKey(
                                new InventoryItemKey(reservation.getSkuId(), reservation.getWarehouseId()))
                                .orElseThrow(() -> new InventoryException(InventoryErrorCode.INVENTORY_ITEM_NOT_FOUND));

                item.release(reservation.getQty(), clock);
                inventoryItemPersistencePort.save(item);

                reservation.release(command.reason(), clock);
                StockReservation saved = stockReservationPersistencePort.save(reservation);

                eventPublisher.publish(reservation.pullEvents());
                eventPublisher.publish(item.pullEvents());
                return saved;
        }

        @Transactional(readOnly = true)
        public StockReservation get(String reservationId) {
                return stockReservationPersistencePort.findById(reservationId)
                                .orElseThrow(() -> new InventoryException(
                                                InventoryErrorCode.STOCK_RESERVATION_NOT_FOUND));
        }

        private int ttlSeconds(ReserveStockCommand command) {
                return command.ttlSeconds() != null && command.ttlSeconds() > 0
                                ? command.ttlSeconds()
                                : reservationPolicy.defaultTtlSeconds();
        }
}
