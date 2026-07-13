package com.aionn.inventory.infrastructure.persistence.adapter.adjustment;

import com.aionn.inventory.application.port.out.StockAdjustmentPersistencePort;
import com.aionn.inventory.domain.model.StockAdjustment;
import com.aionn.inventory.infrastructure.persistence.mapper.StockAdjustmentDomainMapper;
import com.aionn.inventory.infrastructure.persistence.repository.StockAdjustmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StockAdjustmentPersistenceAdapter implements StockAdjustmentPersistencePort {

    private final StockAdjustmentRepository jpa;
    private final StockAdjustmentDomainMapper mapper;

    @Override
    public StockAdjustment save(StockAdjustment adjustment) {
        return mapper.toDomain(jpa.save(mapper.toEntity(adjustment)));
    }
}

