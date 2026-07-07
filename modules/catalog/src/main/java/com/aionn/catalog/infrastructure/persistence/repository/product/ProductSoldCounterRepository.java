package com.aionn.catalog.infrastructure.persistence.repository.product;

import com.aionn.catalog.infrastructure.persistence.entity.ProductSoldCounterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductSoldCounterRepository extends JpaRepository<ProductSoldCounterEntity, String> {

    List<ProductSoldCounterEntity> findAllByProductIdIn(List<String> productIds);
}
