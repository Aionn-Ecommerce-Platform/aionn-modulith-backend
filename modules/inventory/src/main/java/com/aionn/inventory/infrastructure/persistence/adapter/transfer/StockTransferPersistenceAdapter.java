package com.aionn.inventory.infrastructure.persistence.adapter.transfer;

import com.aionn.inventory.application.port.out.StockTransferPersistencePort;
import com.aionn.inventory.domain.model.StockTransfer;
import com.aionn.inventory.infrastructure.persistence.entity.StockTransferEntity;
import com.aionn.inventory.infrastructure.persistence.mapper.StockTransferDomainMapper;
import com.aionn.inventory.infrastructure.persistence.repository.StockTransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StockTransferPersistenceAdapter implements StockTransferPersistencePort {

    private final StockTransferRepository jpa;
    private final StockTransferDomainMapper mapper;

    @Override
    public StockTransfer save(StockTransfer transfer) {
        StockTransferEntity existing = jpa.findById(transfer.getTransferId()).orElse(null);
        StockTransferEntity entity = mapper.toEntity(transfer, existing);
        return mapper.toDomain(jpa.save(entity));
    }

    @Override
    public Optional<StockTransfer> findById(String transferId) {
        return jpa.findById(transferId).map(mapper::toDomain);
    }
}

