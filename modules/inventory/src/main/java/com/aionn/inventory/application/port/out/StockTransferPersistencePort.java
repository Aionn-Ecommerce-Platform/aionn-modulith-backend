package com.aionn.inventory.application.port.out;

import com.aionn.inventory.domain.model.StockTransfer;

import java.util.Optional;

public interface StockTransferPersistencePort {

    StockTransfer save(StockTransfer transfer);

    Optional<StockTransfer> findById(String transferId);
}

