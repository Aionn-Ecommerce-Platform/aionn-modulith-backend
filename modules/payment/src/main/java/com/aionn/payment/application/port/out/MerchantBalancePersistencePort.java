package com.aionn.payment.application.port.out;

import com.aionn.payment.domain.model.MerchantBalance;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface MerchantBalancePersistencePort {

    MerchantBalance save(MerchantBalance balance);

    Optional<MerchantBalance> find(String merchantId, String currency);

    Optional<MerchantBalance> lockForUpdate(String merchantId, String currency);

    MerchantBalance createIfAbsentAndLock(String merchantId, String currency, Instant now);

    long countBalances();

    List<ReconciliationMismatch> findReconciliationMismatches();

    record ReconciliationMismatch(
            String merchantId,
            String currency,
            BigDecimal actualPending,
            BigDecimal ledgerPending,
            BigDecimal actualAvailable,
            BigDecimal ledgerAvailable,
            BigDecimal actualReceivable,
            BigDecimal ledgerReceivable) {
    }
}
