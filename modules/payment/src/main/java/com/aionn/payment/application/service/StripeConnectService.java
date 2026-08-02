package com.aionn.payment.application.service;

import com.aionn.payment.application.port.out.StripeConnectPort;
import com.aionn.payment.domain.exception.PaymentErrorCode;
import com.aionn.payment.domain.exception.PaymentException;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeConnectService {

    private final MerchantQueryPort merchantQueryPort;
    private final StripeConnectPort stripeConnectPort;

    public String createOnboardingLink(String ownerId) {
        String merchantId = merchantQueryPort.findMerchantIdByOwnerId(ownerId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.METHOD_FORBIDDEN,
                        PaymentMessages.NO_MERCHANT_FOR_USER));

        String stripeAccountId = merchantQueryPort.findStripeConnectInfo(merchantId)
                .map(MerchantQueryPort.StripeConnectInfo::stripeAccountId)
                .orElse(null);
        if (stripeAccountId == null) {
            stripeAccountId = stripeConnectPort.createExpressAccount(merchantId);
            merchantQueryPort.saveStripeAccountId(merchantId, stripeAccountId);
        }
        return stripeConnectPort.createOnboardingLink(stripeAccountId);
    }

    public void syncAccountCapabilities(String stripeAccountId) {
        try {
            stripeConnectPort.fetchAccountCapabilities(stripeAccountId).ifPresent(this::applyAccountUpdate);
        } catch (PaymentException ex) {
            log.error("Stripe Connect account fetch failed: {}", stripeAccountId, ex);
        }
    }

    public void applyAccountUpdate(StripeConnectPort.AccountCapabilities account) {
        String merchantId = account.merchantId();
        if (merchantId == null || merchantId.isBlank()) {
            log.warn("Stripe account {} has no merchantId metadata, skipping sync", account.stripeAccountId());
            return;
        }
        boolean charges = account.chargesEnabled();
        boolean payouts = account.payoutsEnabled();
        merchantQueryPort.updateStripeCapabilities(merchantId, charges, payouts);
        log.info("Stripe Connect synced merchant={} charges={} payouts={}",
                merchantId, charges, payouts);
    }

}
