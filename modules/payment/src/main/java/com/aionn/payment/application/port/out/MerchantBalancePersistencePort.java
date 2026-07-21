package com.aionn.payment.application.port.out;

import com.aionn.payment.domain.model.MerchantBalance;

import java.util.Optional;

public interface MerchantBalancePersistencePort {

    MerchantBalance save(MerchantBalance balance);

    Optional<MerchantBalance> find(String merchantId, String currency);

    Optional<MerchantBalance> lockForUpdate(String merchantId, String currency);
}
