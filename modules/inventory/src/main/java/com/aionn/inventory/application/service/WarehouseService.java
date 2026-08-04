package com.aionn.inventory.application.service;

import com.aionn.inventory.application.dto.warehouse.command.AdjustPriorityCommand;
import com.aionn.inventory.application.dto.warehouse.command.ChangeStatusCommand;
import com.aionn.inventory.application.dto.warehouse.command.CreateWarehouseCommand;
import com.aionn.inventory.application.dto.warehouse.command.LiftSuspensionCommand;
import com.aionn.inventory.application.dto.warehouse.command.SuspendWarehouseCommand;
import com.aionn.inventory.application.port.out.WarehousePersistencePort;
import com.aionn.inventory.domain.exception.InventoryErrorCode;
import com.aionn.inventory.domain.exception.InventoryException;
import com.aionn.inventory.domain.model.Warehouse;
import com.aionn.inventory.domain.valueobject.WarehouseStatus;
import com.aionn.sharedkernel.application.port.EventPublisher;
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
    private final EventPublisher eventPublisher;
    private final Clock clock;

    public Warehouse create(CreateWarehouseCommand command) {
        Warehouse warehouse = Warehouse.create(IdGenerator.ulid(),
                command.ownerId(), command.address(), command.priorityLevel(), clock);
        Warehouse saved = warehouseRepository.save(warehouse);
        eventPublisher.publish(warehouse.pullEvents());
        return saved;
    }

    public Warehouse changeStatus(ChangeStatusCommand command) {
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
        return saved;
    }

    public Warehouse adjustPriority(AdjustPriorityCommand command) {
        Warehouse warehouse = ownedByOwner(command.warehouseId(), command.ownerId());
        warehouse.adjustPriority(command.priorityLevel(), clock);
        Warehouse saved = warehouseRepository.save(warehouse);
        eventPublisher.publish(warehouse.pullEvents());
        return saved;
    }

    public Warehouse suspend(SuspendWarehouseCommand command) {
        Warehouse warehouse = required(command.warehouseId());
        warehouse.suspend(command.adminId(), command.reason(), clock);
        Warehouse saved = warehouseRepository.save(warehouse);
        eventPublisher.publish(warehouse.pullEvents());
        return saved;
    }

    public Warehouse liftSuspension(LiftSuspensionCommand command) {
        Warehouse warehouse = required(command.warehouseId());
        warehouse.liftSuspension(clock);
        Warehouse saved = warehouseRepository.save(warehouse);
        eventPublisher.publish(warehouse.pullEvents());
        return saved;
    }

    @Transactional(readOnly = true)
    public Warehouse get(String warehouseId) {
        return required(warehouseId);
    }

    @Transactional(readOnly = true)
    public Warehouse getOwned(String warehouseId, String merchantId) {
        Warehouse warehouse = required(warehouseId);
        warehouse.ensureOwnedBy(merchantId);
        return warehouse;
    }

    @Transactional(readOnly = true)
    public List<Warehouse> listByOwner(String ownerId) {
        return warehouseRepository.findByMerchantOrderByPriority(ownerId);
    }

    private Warehouse required(String warehouseId) {
        return warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.WAREHOUSE_NOT_FOUND));
    }

    Warehouse ownedByOwner(String warehouseId, String ownerId) {
        Warehouse warehouse = required(warehouseId);
        warehouse.ensureOwnedBy(ownerId);
        return warehouse;
    }
}
