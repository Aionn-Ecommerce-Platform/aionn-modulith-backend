package com.aionn.payment.infrastructure.provider;

import com.aionn.payment.application.port.out.stripeconnect.StripeConnectWebhookPort;
import com.aionn.payment.infrastructure.provider.config.StripeProperties;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Account;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StripeConnectWebhookAdapter implements StripeConnectWebhookPort {

    private final StripeProperties stripeProperties;

    @Override
    public WebhookEvent parseAndVerify(String rawBody, String signature) {
        if (signature == null || stripeProperties.webhookSecret() == null
                || stripeProperties.webhookSecret().isBlank()) {
            log.warn("Stripe Connect webhook missing signature or secret not configured");
            return null;
        }
        Event event;
        try {
            event = Webhook.constructEvent(rawBody, signature, stripeProperties.webhookSecret());
        } catch (SignatureVerificationException ex) {
            log.warn("Stripe Connect webhook signature mismatch: {}", ex.getMessage());
            return null;
        }
        Account account = event.getDataObjectDeserializer().getObject()
                .filter(Account.class::isInstance)
                .map(Account.class::cast)
                .orElse(null);
        if (account == null) {
            log.warn("Stripe Connect webhook payload could not be parsed");
            return new WebhookEvent(event.getType(), null, false, false);
        }
        boolean charges = Boolean.TRUE.equals(account.getChargesEnabled());
        boolean payouts = Boolean.TRUE.equals(account.getPayoutsEnabled());
        return new WebhookEvent(event.getType(), account.getId(), charges, payouts);
    }
}
