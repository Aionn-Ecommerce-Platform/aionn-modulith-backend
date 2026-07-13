package com.aionn.inventory.infrastructure.persistence.repository;

import com.aionn.inventory.infrastructure.persistence.entity.StockTransferEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockTransferRepository extends JpaRepository<StockTransferEntity, String> {
}

