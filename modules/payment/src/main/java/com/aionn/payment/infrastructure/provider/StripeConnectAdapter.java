package com.aionn.payment.infrastructure.provider;

import com.aionn.payment.application.port.out.StripeConnectPort;
import com.aionn.payment.domain.exception.PaymentErrorCode;
import com.aionn.payment.domain.exception.PaymentException;
import com.aionn.payment.infrastructure.provider.config.StripeConnectProperties;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StripeConnectAdapter implements StripeConnectPort {

    private final StripeConnectProperties properties;

    @Override
    public String createExpressAccount(String merchantId) {
        try {
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
            return account.getId();
        } catch (StripeException ex) {
            throw providerFailure(ex);
        }
    }

    @Override
    public String createOnboardingLink(String stripeAccountId) {
        try {
            AccountLink link = AccountLink.create(AccountLinkCreateParams.builder()
                    .setAccount(stripeAccountId)
                    .setRefreshUrl(properties.refreshUrl())
                    .setReturnUrl(properties.returnUrl())
                    .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                    .build());
            return link.getUrl();
        } catch (StripeException ex) {
            throw providerFailure(ex);
        }
    }

    @Override
    public Optional<AccountCapabilities> fetchAccountCapabilities(String stripeAccountId) {
        try {
            Account account = Account.retrieve(stripeAccountId);
            String merchantId = account.getMetadata() == null ? null : account.getMetadata().get("merchantId");
            return Optional.of(new AccountCapabilities(account.getId(), merchantId,
                    Boolean.TRUE.equals(account.getChargesEnabled()),
                    Boolean.TRUE.equals(account.getPayoutsEnabled())));
        } catch (StripeException ex) {
            throw providerFailure(ex);
        }
    }

    private PaymentException providerFailure(StripeException ex) {
        return new PaymentException(PaymentErrorCode.PAYMENT_GATEWAY_ERROR,
                "Stripe Connect error: " + ex.getMessage());
    }
}
