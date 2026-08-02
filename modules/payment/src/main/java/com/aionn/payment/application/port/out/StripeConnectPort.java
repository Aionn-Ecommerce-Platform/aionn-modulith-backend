package com.aionn.payment.application.port.out;

import java.util.Optional;

public interface StripeConnectPort {

    String createExpressAccount(String merchantId);

    String createOnboardingLink(String stripeAccountId);

    Optional<AccountCapabilities> fetchAccountCapabilities(String stripeAccountId);

    record AccountCapabilities(String stripeAccountId, String merchantId,
            boolean chargesEnabled, boolean payoutsEnabled) {}
}
