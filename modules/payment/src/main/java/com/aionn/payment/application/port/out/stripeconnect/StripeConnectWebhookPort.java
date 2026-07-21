package com.aionn.payment.application.port.out.stripeconnect;

public interface StripeConnectWebhookPort {

    WebhookEvent parseAndVerify(String rawBody, String signature);

    record WebhookEvent(String type, String stripeAccountId, boolean chargesEnabled, boolean payoutsEnabled) {
    }
}
