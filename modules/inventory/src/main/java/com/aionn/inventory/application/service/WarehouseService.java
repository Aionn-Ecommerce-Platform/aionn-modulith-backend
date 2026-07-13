package com.aionn.inventory.application.service;

import com.aionn.inventory.application.dto.warehouse.command.AdjustPriorityCommand;
import com.aionn.inventory.application.dto.warehouse.command.ChangeStatusCommand;
import com.aionn.inventory.application.dto.warehouse.command.CreateWarehouseCommand;
import com.aionn.inventory.application.dto.warehouse.command.LiftSuspensionCommand;
import com.aionn.inventory.application.dto.warehouse.command.SuspendWarehouseCommand;
import com.aionn.inventory.application.dto.warehouse.result.WarehouseResult;
import com.aionn.inventory.application.mapper.InventoryResultMapper;
import com.aionn.inventory.application.port.out.WarehousePersistencePort;
import com.aionn.inventory.domain.exception.InventoryErrorCode;
import com.aionn.inventory.domain.exception.InventoryException;
import com.aionn.inventory.domain.model.Warehouse;
import com.aionn.inventory.domain.valueobject.WarehouseStatus;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class WarehouseService {

    private final WarehousePersistencePort warehouseRepository;
    private final InventoryResultMapper mapper;
    private final EventPublisher eventPublisher;
    private final MerchantQueryPort merchantQueryPort;
    private final Clock clock;

    public WarehouseResult create(CreateWarehouseCommand command) {
        String merchantId = requireMerchantIdForOwner(command.ownerId());
        Warehouse warehouse = Warehouse.create(IdGenerator.ulid(),
                merchantId, command.address(), command.priorityLevel(), clock);
        Warehouse saved = warehouseRepository.save(warehouse);
        eventPublisher.publish(warehouse.pullEvents());
        return mapper.toResult(saved);
    }

    public WarehouseResult changeStatus(ChangeStatusCommand command) {
        Warehouse warehouse = ownedByOwner(command.warehouseId(), command.ownerId());
        WarehouseStatus next;
        try {
            next = WarehouseStatus.valueOf(command.status());
        } catch (IllegalArgumentException ex) {
            throw new InventoryException(InventoryErrorCode.INVALID_ARGUMENT,
                    "Unknown status: " + command.status());
        }
        warehouse.changeStatus(next, clock);
        Warehouse saved = warehouseRepository.save(warehouse);
        eventPublisher.publish(warehouse.pullEvents());
        return mapper.toResult(saved);
    }

    public WarehouseResult adjustPriority(AdjustPriorityCommand command) {
        Warehouse warehouse = ownedByOwner(command.warehouseId(), command.ownerId());
        warehouse.adjustPriority(command.priorityLevel(), clock);
        Warehouse saved = warehouseRepository.save(warehouse);
        eventPublisher.publish(warehouse.pullEvents());
        return mapper.toResult(saved);
    }

    public WarehouseResult suspend(SuspendWarehouseCommand command) {
        Warehouse warehouse = required(command.warehouseId());
        warehouse.suspend(command.adminId(), command.reason(), clock);
        Warehouse saved = warehouseRepository.save(warehouse);
        eventPublisher.publish(warehouse.pullEvents());
        return mapper.toResult(saved);
    }

    public WarehouseResult liftSuspension(LiftSuspensionCommand command) {
        Warehouse warehouse = required(command.warehouseId());
        warehouse.liftSuspension(clock);
        Warehouse saved = warehouseRepository.save(warehouse);
        eventPublisher.publish(warehouse.pullEvents());
        return mapper.toResult(saved);
    }

    @Transactional(readOnly = true)
    public WarehouseResult get(String warehouseId) {
        return mapper.toResult(required(warehouseId));
    }

    @Transactional(readOnly = true)
    public List<WarehouseResult> listByOwner(String ownerId) {
        String merchantId = requireMerchantIdForOwner(ownerId);
        return warehouseRepository.findByMerchantOrderByPriority(merchantId).stream()
                .map(mapper::toResult)
                .toList();
    }

    private Warehouse required(String warehouseId) {
        return warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.WAREHOUSE_NOT_FOUND));
    }

    Warehouse ownedByOwner(String warehouseId, String ownerId) {
        String merchantId = requireMerchantIdForOwner(ownerId);
        Warehouse warehouse = required(warehouseId);
        warehouse.ensureOwnedBy(merchantId);
        return warehouse;
    }

    String requireMerchantIdForOwner(String ownerId) {
        return merchantQueryPort.findMerchantIdByOwnerId(ownerId)
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.WAREHOUSE_FORBIDDEN,
                        "No merchant registered for the authenticated user"));
    }
}
