package com.aionn.payment.application.port.out;

import com.aionn.payment.domain.model.MerchantPayout;
import com.aionn.payment.domain.valueobject.PayoutStatus;

import java.util.List;
import java.util.Optional;

public interface MerchantPayoutPersistencePort {

    MerchantPayout save(MerchantPayout payout);

    Optional<MerchantPayout> findById(String payoutId);

    List<MerchantPayout> findByMerchant(String merchantId, int limit);

    List<MerchantPayout> findByStatus(PayoutStatus status, int limit);
}
