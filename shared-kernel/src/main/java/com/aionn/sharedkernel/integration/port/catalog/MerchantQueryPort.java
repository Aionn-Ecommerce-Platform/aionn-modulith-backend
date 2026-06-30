package com.aionn.sharedkernel.integration.port.catalog;

import java.math.BigDecimal;
import java.util.Optional;

public interface MerchantQueryPort {

    Optional<String> findMerchantIdByOwnerId(String ownerId);

    Optional<String> findOwnerIdByMerchantId(String merchantId);

    Optional<BigDecimal> findCommissionRate(String merchantId);

    Optional<StripeConnectInfo> findStripeConnectInfo(String merchantId);

    record StripeConnectInfo(String stripeAccountId, boolean chargesEnabled, boolean payoutsEnabled) {
    }
}
