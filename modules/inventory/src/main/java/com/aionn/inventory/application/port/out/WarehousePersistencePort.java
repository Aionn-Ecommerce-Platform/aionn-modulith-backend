package com.aionn.inventory.application.port.out;

import com.aionn.inventory.domain.model.Warehouse;

import java.util.List;
import java.util.Optional;

public interface WarehousePersistencePort {

    Warehouse save(Warehouse warehouse);

    Optional<Warehouse> findById(String warehouseId);

    List<Warehouse> findByMerchantOrderByPriority(String merchantId);
}

