package com.aionn.payment.application.service;

import com.aionn.payment.application.port.in.stripeconnect.SyncStripeConnectAccountInputPort;
import com.aionn.payment.application.port.out.stripeconnect.StripeConnectWebhookPort;
import com.aionn.payment.domain.valueobject.StripeConnectEventType;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeConnectWebhookService {

    private final StripeConnectWebhookPort stripeConnectWebhookPort;
    private final SyncStripeConnectAccountInputPort syncStripeConnectAccountInputPort;

    public boolean handle(byte[] rawBody, String signature) {
        StripeConnectWebhookPort.WebhookEvent event = stripeConnectWebhookPort.parseAndVerify(
                new String(rawBody, StandardCharsets.UTF_8), signature);
        if (event == null) {
            return false;
        }
        if (!StripeConnectEventType.ACCOUNT_UPDATED.eventType().equals(event.type())) {
            return true;
        }
        if (event.stripeAccountId() == null) {
            log.warn("Stripe Connect webhook payload could not be parsed");
            return false;
        }
        syncStripeConnectAccountInputPort.execute(
                event.stripeAccountId(), event.chargesEnabled(), event.payoutsEnabled());
        return true;
    }
}
