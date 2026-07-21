package com.aionn.payment.application.usecase.stripeconnect;

import com.aionn.payment.application.port.in.stripeconnect.SyncStripeConnectAccountInputPort;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncStripeConnectAccountUseCase implements SyncStripeConnectAccountInputPort {

    private final MerchantQueryPort merchantQueryPort;

    @Override
    public void execute(String stripeAccountId, boolean chargesEnabled, boolean payoutsEnabled) {
        if (stripeAccountId == null || stripeAccountId.isBlank()) {
            log.warn("SyncStripeConnectAccount: stripeAccountId is null, skipping sync");
            return;
        }
        merchantQueryPort.updateStripeCapabilities(stripeAccountId, chargesEnabled, payoutsEnabled);
        log.info("Stripe Connect synced stripeAccountId={} charges={} payouts={}",
                stripeAccountId, chargesEnabled, payoutsEnabled);
    }
}
