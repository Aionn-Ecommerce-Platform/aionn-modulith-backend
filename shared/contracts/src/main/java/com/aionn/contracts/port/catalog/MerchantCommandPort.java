package com.aionn.contracts.port.catalog;

public interface MerchantCommandPort {

    void saveStripeAccountId(String merchantId, String stripeAccountId);

    void updateStripeCapabilities(String merchantId, boolean chargesEnabled, boolean payoutsEnabled);
}
