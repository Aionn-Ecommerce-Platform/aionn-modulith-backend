package com.aionn.payment.application.port.out;

import com.aionn.payment.domain.model.SettlementLedgerEntry;

import java.util.List;

public interface SettlementLedgerPersistencePort {

    SettlementLedgerEntry save(SettlementLedgerEntry entry);

    List<SettlementLedgerEntry> findByMerchant(String merchantId, int limit);

    List<SettlementLedgerEntry> findByOrder(String orderId);
}
