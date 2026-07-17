package com.aionn.inventory.application.service;

import com.aionn.inventory.application.dto.reservation.command.CommitReservationCommand;
import com.aionn.inventory.application.dto.reservation.command.ReleaseReservationCommand;
import com.aionn.inventory.application.dto.reservation.command.ReserveStockCommand;
import com.aionn.inventory.application.dto.reservation.result.ReservationResult;
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

        private final InventoryItemPersistencePort itemRepository;
        private final StockReservationPersistencePort reservationRepository;
        private final StockAdjustmentPersistencePort adjustmentRepository;
        private final InventoryResultMapper mapper;
        private final EventPublisher eventPublisher;
        private final OutboundOrderNotifier outboundOrderNotifier;
        private final Clock clock;

        public ReservationResult reserve(ReserveStockCommand command) {
                InventoryItemKey key = new InventoryItemKey(command.skuId(), command.warehouseId());
                InventoryItem item = itemRepository.lockByKey(key)
                                .orElseThrow(() -> new InventoryException(InventoryErrorCode.INVENTORY_ITEM_NOT_FOUND));

                if (item.isLocked() || item.getAvailableQty() < command.qty()) {
                        String reason = item.isLocked() ? "Inventory locked" : "Insufficient stock";
                        StockReservation failed = StockReservation.failed(IdGenerator.ulid(),
                                        command.skuId(), command.warehouseId(), command.qty(), reason, clock);
                        StockReservation saved = reservationRepository.save(failed);
                        eventPublisher.publish(failed.pullEvents());
                        outboundOrderNotifier.notifyReservationFailed(command.orderId(),
                                        command.skuId(), command.warehouseId(), command.qty(), reason);
                        return mapper.toResult(saved);
                }

                item.reserve(command.qty(), clock);
                itemRepository.save(item);

                Instant expiresAt = clock.instant().plus(Duration.ofSeconds(command.ttlSeconds()));
                StockReservation reservation = StockReservation.reserve(IdGenerator.ulid(),
                                command.skuId(), command.warehouseId(), command.orderId(), command.qty(), expiresAt, clock);
                StockReservation saved = reservationRepository.save(reservation);
                eventPublisher.publish(reservation.pullEvents());
                eventPublisher.publish(item.pullEvents());
                return mapper.toResult(saved);
        }

        public ReservationResult commit(CommitReservationCommand command) {
                StockReservation reservation = reservationRepository.findById(command.reservationId())
                                .orElseThrow(() -> new InventoryException(
                                                InventoryErrorCode.STOCK_RESERVATION_NOT_FOUND));
                if (reservation.getStatus() != ReservationStatus.RESERVED) {
                        throw new InventoryException(InventoryErrorCode.STOCK_RESERVATION_INVALID_STATE);
                }
                InventoryItem item = itemRepository.lockByKey(
                                new InventoryItemKey(reservation.getSkuId(), reservation.getWarehouseId()))
                                .orElseThrow(() -> new InventoryException(InventoryErrorCode.INVENTORY_ITEM_NOT_FOUND));

                item.commit(reservation.getQty(), clock);
                itemRepository.save(item);

                reservation.commit(clock);
                StockReservation saved = reservationRepository.save(reservation);

                StockAdjustment outbound = StockAdjustment.outbound(IdGenerator.ulid(),
                                reservation.getSkuId(), reservation.getWarehouseId(), reservation.getQty(),
                                reservation.getOrderId(), clock);
                adjustmentRepository.save(outbound);

                eventPublisher.publish(reservation.pullEvents());
                eventPublisher.publish(item.pullEvents());
                eventPublisher.publish(outbound.pullEvents());
                outboundOrderNotifier.notifyOutbound(reservation.getOrderId(), reservation.getReservationId(),
                                reservation.getSkuId(), reservation.getWarehouseId(), reservation.getQty());
                return mapper.toResult(saved);
        }

        public ReservationResult release(ReleaseReservationCommand command) {
                StockReservation reservation = reservationRepository.findById(command.reservationId())
                                .orElseThrow(() -> new InventoryException(
                                                InventoryErrorCode.STOCK_RESERVATION_NOT_FOUND));
                if (reservation.getStatus() != ReservationStatus.RESERVED) {
                        throw new InventoryException(InventoryErrorCode.STOCK_RESERVATION_INVALID_STATE);
                }
                InventoryItem item = itemRepository.lockByKey(
                                new InventoryItemKey(reservation.getSkuId(), reservation.getWarehouseId()))
                                .orElseThrow(() -> new InventoryException(InventoryErrorCode.INVENTORY_ITEM_NOT_FOUND));

                item.release(reservation.getQty(), clock);
                itemRepository.save(item);

                reservation.release(command.reason(), clock);
                StockReservation saved = reservationRepository.save(reservation);

                eventPublisher.publish(reservation.pullEvents());
                eventPublisher.publish(item.pullEvents());
                return mapper.toResult(saved);
        }

        @Transactional(readOnly = true)
        public ReservationResult get(String reservationId) {
                return mapper.toResult(reservationRepository.findById(reservationId)
                                .orElseThrow(() -> new InventoryException(
                                                InventoryErrorCode.STOCK_RESERVATION_NOT_FOUND)));
        }
}
