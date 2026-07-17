package com.aionn.inventory.application.service;

import com.aionn.inventory.application.dto.transfer.command.CancelTransferCommand;
import com.aionn.inventory.application.dto.transfer.command.CompleteTransferCommand;
import com.aionn.inventory.application.dto.transfer.command.InitiateTransferCommand;
import com.aionn.inventory.application.dto.transfer.result.StockTransferResult;
import com.aionn.inventory.application.mapper.InventoryResultMapper;
import com.aionn.inventory.application.port.out.InventoryItemPersistencePort;
import com.aionn.inventory.application.port.out.StockAdjustmentPersistencePort;
import com.aionn.inventory.application.port.out.StockTransferPersistencePort;
import com.aionn.inventory.application.port.out.WarehousePersistencePort;
import com.aionn.inventory.domain.exception.InventoryErrorCode;
import com.aionn.inventory.domain.exception.InventoryException;
import com.aionn.inventory.domain.model.InventoryItem;
import com.aionn.inventory.domain.model.StockAdjustment;
import com.aionn.inventory.domain.model.StockTransfer;
import com.aionn.inventory.domain.model.Warehouse;
import com.aionn.inventory.domain.valueobject.AdjustmentType;
import com.aionn.inventory.domain.valueobject.InventoryItemKey;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StockTransferService {

        private final WarehousePersistencePort warehouseRepository;
        private final InventoryItemPersistencePort itemRepository;
        private final StockTransferPersistencePort transferRepository;
        private final StockAdjustmentPersistencePort adjustmentRepository;
        private final InventoryResultMapper mapper;
        private final EventPublisher eventPublisher;
        private final MerchantQueryPort merchantQueryPort;
        private final Clock clock;

        public StockTransferResult initiate(InitiateTransferCommand command) {
                String merchantId = requireMerchantIdForOwner(command.ownerId());
                Warehouse from = warehouseRepository.findById(command.fromWarehouseId())
                                .orElseThrow(() -> new InventoryException(InventoryErrorCode.WAREHOUSE_NOT_FOUND));
                Warehouse to = warehouseRepository.findById(command.toWarehouseId())
                                .orElseThrow(() -> new InventoryException(InventoryErrorCode.WAREHOUSE_NOT_FOUND));
                if (!from.getMerchantId().equals(merchantId) || !to.getMerchantId().equals(merchantId)) {
                        throw new InventoryException(InventoryErrorCode.STOCK_TRANSFER_DIFFERENT_MERCHANT);
                }
                if (!from.getStatus().canFulfill()) {
                        throw new InventoryException(InventoryErrorCode.WAREHOUSE_INVALID_TRANSITION,
                                        "Source warehouse must be ACTIVE to initiate a transfer");
                }

                InventoryItem source = itemRepository.lockByKey(
                                new InventoryItemKey(command.skuId(), command.fromWarehouseId()))
                                .orElseThrow(() -> new InventoryException(InventoryErrorCode.INVENTORY_ITEM_NOT_FOUND));
                source.adjust(-command.qty(), AdjustmentType.TRANSFER_OUT, "transfer-out", clock);
                itemRepository.save(source);
                eventPublisher.publish(source.pullEvents());

                StockTransfer transfer = StockTransfer.initiate(IdGenerator.ulid(),
                                merchantId, command.fromWarehouseId(), command.toWarehouseId(),
                                command.skuId(), command.qty(), clock);
                StockTransfer saved = transferRepository.save(transfer);

                StockAdjustment outAdj = StockAdjustment.manual(IdGenerator.ulid(),
                                command.skuId(), command.fromWarehouseId(), command.qty(),
                                AdjustmentType.TRANSFER_OUT, "transfer " + saved.getTransferId(), clock);
                adjustmentRepository.save(outAdj);

                eventPublisher.publish(transfer.pullEvents());
                eventPublisher.publish(outAdj.pullEvents());
                return mapper.toResult(saved);
        }

        public StockTransferResult complete(CompleteTransferCommand command) {
                StockTransfer transfer = ownedByOwner(command.transferId(), command.ownerId());
                transfer.complete(command.receivedQty(), clock);

                InventoryItemKey destKey = new InventoryItemKey(transfer.getSkuId(), transfer.getToWarehouseId());
                InventoryItem dest = itemRepository.lockByKey(destKey)
                                .orElseGet(() -> itemRepository.save(InventoryItem.initialize(destKey, 0, clock)));
                dest.adjust(command.receivedQty(), AdjustmentType.TRANSFER_IN, "transfer-in", clock);
                itemRepository.save(dest);
                eventPublisher.publish(dest.pullEvents());

                StockTransfer saved = transferRepository.save(transfer);

                StockAdjustment inAdj = StockAdjustment.manual(IdGenerator.ulid(),
                                transfer.getSkuId(), transfer.getToWarehouseId(), command.receivedQty(),
                                AdjustmentType.TRANSFER_IN, "transfer " + transfer.getTransferId(), clock);
                adjustmentRepository.save(inAdj);

                eventPublisher.publish(transfer.pullEvents());
                eventPublisher.publish(inAdj.pullEvents());
                return mapper.toResult(saved);
        }

        public StockTransferResult cancel(CancelTransferCommand command) {
                StockTransfer transfer = ownedByOwner(command.transferId(), command.ownerId());
                transfer.cancel(command.reason(), clock);

                InventoryItem source = itemRepository.lockByKey(
                                new InventoryItemKey(transfer.getSkuId(), transfer.getFromWarehouseId()))
                                .orElseThrow(() -> new InventoryException(InventoryErrorCode.INVENTORY_ITEM_NOT_FOUND));
                source.adjust(transfer.getQty(), AdjustmentType.TRANSFER_IN, "transfer-cancelled", clock);
                itemRepository.save(source);

                StockTransfer saved = transferRepository.save(transfer);
                eventPublisher.publish(transfer.pullEvents());
                eventPublisher.publish(source.pullEvents());
                return mapper.toResult(saved);
        }

        @Transactional(readOnly = true)
        public StockTransferResult get(String transferId) {
                return mapper.toResult(transferRepository.findById(transferId)
                                .orElseThrow(() -> new InventoryException(
                                                InventoryErrorCode.STOCK_TRANSFER_NOT_FOUND)));
        }

        private StockTransfer ownedByOwner(String transferId, String ownerId) {
                String merchantId = requireMerchantIdForOwner(ownerId);
                StockTransfer transfer = transferRepository.findById(transferId)
                                .orElseThrow(() -> new InventoryException(InventoryErrorCode.STOCK_TRANSFER_NOT_FOUND));
                if (!transfer.getMerchantId().equals(merchantId)) {
                        throw new InventoryException(InventoryErrorCode.STOCK_TRANSFER_DIFFERENT_MERCHANT);
                }
                return transfer;
        }

        private String requireMerchantIdForOwner(String ownerId) {
                return merchantQueryPort.findMerchantIdByOwnerId(ownerId)
                                .orElseThrow(() -> new InventoryException(InventoryErrorCode.WAREHOUSE_FORBIDDEN,
                                                "No merchant registered for the authenticated user"));
        }
}
