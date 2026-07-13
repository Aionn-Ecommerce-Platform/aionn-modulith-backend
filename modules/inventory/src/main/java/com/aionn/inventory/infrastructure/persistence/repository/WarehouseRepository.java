package com.aionn.inventory.infrastructure.persistence.repository;

import com.aionn.inventory.infrastructure.persistence.entity.WarehouseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WarehouseRepository extends JpaRepository<WarehouseEntity, String> {

    List<WarehouseEntity> findByMerchantIdOrderByPriorityLevelAsc(String merchantId);
}

