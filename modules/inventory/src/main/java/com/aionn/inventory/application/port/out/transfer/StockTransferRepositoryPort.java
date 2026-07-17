package com.aionn.inventory.application.port.out.transfer;

import com.aionn.inventory.domain.model.StockTransfer;
import com.aionn.inventory.domain.valueobject.StockTransferStatus;

import java.util.List;
import java.util.Optional;

public interface StockTransferRepositoryPort {

    StockTransfer save(StockTransfer transfer);

    Optional<StockTransfer> findById(String transferId);

    List<StockTransfer> findByMerchantAndStatus(String merchantId, StockTransferStatus status, int page, int size);
}
