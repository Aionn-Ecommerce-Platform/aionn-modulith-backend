package com.aionn.payment.application.service;

import com.aionn.payment.domain.exception.PaymentErrorCode;
import com.aionn.payment.domain.exception.PaymentException;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeConnectService {

    private final MerchantQueryPort merchantQueryPort;

    @Value("${payment.stripe.connect.refresh-url:http://localhost:3000/merchant/settings/stripe/refresh}")
    private String refreshUrl;

    @Value("${payment.stripe.connect.return-url:http://localhost:3000/merchant/settings/stripe/return}")
    private String returnUrl;

    public String createOnboardingLink(String ownerId) {
        String merchantId = merchantQueryPort.findMerchantIdByOwnerId(ownerId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.METHOD_FORBIDDEN,
                        "No merchant for current user"));

        try {
            String stripeAccountId = merchantQueryPort.findStripeConnectInfo(merchantId)
                    .map(MerchantQueryPort.StripeConnectInfo::stripeAccountId)
                    .orElse(null);
            if (stripeAccountId == null) {
                stripeAccountId = createExpressAccount(merchantId);
            }

            AccountLink link = AccountLink.create(AccountLinkCreateParams.builder()
                    .setAccount(stripeAccountId)
                    .setRefreshUrl(refreshUrl)
                    .setReturnUrl(returnUrl)
                    .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                    .build());
            return link.getUrl();
        } catch (StripeException ex) {
            log.error("Stripe Connect onboarding failed for merchant {}", merchantId, ex);
            throw new PaymentException(PaymentErrorCode.PAYMENT_GATEWAY_ERROR,
                    "Stripe Connect error: " + ex.getMessage());
        }
    }

    public void syncAccountCapabilities(String stripeAccountId) {
        try {
            Account account = Account.retrieve(stripeAccountId);
            applyAccountUpdate(account);
        } catch (StripeException ex) {
            log.error("Stripe Connect account fetch failed: {}", stripeAccountId, ex);
        }
    }

    public void applyAccountUpdate(Account account) {
        String merchantId = account.getMetadata() == null
                ? null
                : account.getMetadata().get("merchantId");
        if (merchantId == null || merchantId.isBlank()) {
            log.warn("Stripe account {} has no merchantId metadata, skipping sync", account.getId());
            return;
        }
        boolean charges = Boolean.TRUE.equals(account.getChargesEnabled());
        boolean payouts = Boolean.TRUE.equals(account.getPayoutsEnabled());
        merchantQueryPort.updateStripeCapabilities(merchantId, charges, payouts);
        log.info("Stripe Connect synced merchant={} charges={} payouts={}",
                merchantId, charges, payouts);
    }

    private String createExpressAccount(String merchantId) throws StripeException {
        Account account = Account.create(AccountCreateParams.builder()
                .setType(AccountCreateParams.Type.EXPRESS)
                .putMetadata("merchantId", merchantId)
                .setCapabilities(AccountCreateParams.Capabilities.builder()
                        .setCardPayments(AccountCreateParams.Capabilities.CardPayments.builder()
                                .setRequested(true).build())
                        .setTransfers(AccountCreateParams.Capabilities.Transfers.builder()
                                .setRequested(true).build())
                        .build())
                .build());
        merchantQueryPort.saveStripeAccountId(merchantId, account.getId());
        log.info("Stripe Connect: created Express account {} for merchant {}",
                account.getId(), merchantId);
        return account.getId();
    }
}
