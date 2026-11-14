package com.aionn.payment.application.service;

import com.aionn.payment.application.port.in.stripeconnect.SyncStripeConnectAccountInputPort;
import com.aionn.payment.application.port.out.stripeconnect.StripeConnectWebhookPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StripeConnectWebhookServiceTest {

    @Mock
    private StripeConnectWebhookPort stripeConnectWebhookPort;
    @Mock
    private SyncStripeConnectAccountInputPort syncStripeConnectAccountInputPort;

    @InjectMocks
    private StripeConnectWebhookService service;

    @Test
    void syncsAccountOnAccountUpdated() {
        StripeConnectWebhookPort.WebhookEvent event =
                new StripeConnectWebhookPort.WebhookEvent("account.updated", "acct-1", true, false);
        when(stripeConnectWebhookPort.parseAndVerify(any(), any())).thenReturn(event);

        assertTrue(service.handle(new byte[0], "sig"));

        verify(syncStripeConnectAccountInputPort).execute("acct-1", true, false);
    }

    @Test
    void ignoresOtherEventTypes() {
        StripeConnectWebhookPort.WebhookEvent event =
                new StripeConnectWebhookPort.WebhookEvent("payment_intent.created", "acct-1", true, true);
        when(stripeConnectWebhookPort.parseAndVerify(any(), any())).thenReturn(event);

        assertTrue(service.handle(new byte[0], "sig"));

        verifyNoInteractions(syncStripeConnectAccountInputPort);
    }

    @Test
    void rejectsUnparseablePayload() {
        when(stripeConnectWebhookPort.parseAndVerify(any(), any())).thenReturn(null);

        assertFalse(service.handle(new byte[0], "sig"));
        verifyNoInteractions(syncStripeConnectAccountInputPort);
    }

    @Test
    void rejectsAccountUpdatedWithoutAccountId() {
        StripeConnectWebhookPort.WebhookEvent event =
                new StripeConnectWebhookPort.WebhookEvent("account.updated", null, false, false);
        when(stripeConnectWebhookPort.parseAndVerify(any(), any())).thenReturn(event);

        assertFalse(service.handle(new byte[0], "sig"));
        verifyNoInteractions(syncStripeConnectAccountInputPort);
    }
}
