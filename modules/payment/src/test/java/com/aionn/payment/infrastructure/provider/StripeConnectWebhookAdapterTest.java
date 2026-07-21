package com.aionn.payment.infrastructure.provider;

import com.aionn.payment.application.port.out.stripeconnect.StripeConnectWebhookPort;
import com.aionn.payment.infrastructure.provider.config.StripeProperties;
import com.stripe.model.Account;
import com.stripe.model.Event;
import com.stripe.model.HasId;
import com.stripe.model.StripeObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import com.stripe.net.Webhook;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StripeConnectWebhookAdapterTest {

    @Mock
    private StripeProperties stripeProperties;

    private StripeConnectWebhookAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new StripeConnectWebhookAdapter(stripeProperties);
    }

    @Test
    void parseAndVerifyShouldReturnNullWhenSignatureIsMissing() {
        StripeConnectWebhookPort.WebhookEvent result = adapter.parseAndVerify("{}", null);
        assertNull(result);
    }

    @Test
    void parseAndVerifyShouldReturnNullWhenWebhookSecretIsNull() {
        when(stripeProperties.webhookSecret()).thenReturn(null);
        StripeConnectWebhookPort.WebhookEvent result = adapter.parseAndVerify("{}", "sig_abc");
        assertNull(result);
    }

    @Test
    void parseAndVerifyShouldReturnNullWhenWebhookSecretIsBlank() {
        when(stripeProperties.webhookSecret()).thenReturn("  ");
        StripeConnectWebhookPort.WebhookEvent result = adapter.parseAndVerify("{}", "sig_abc");
        assertNull(result);
    }

    @Test
    void parseAndVerifyShouldReturnNullOnSignatureMismatch() {
        when(stripeProperties.webhookSecret()).thenReturn("whsec_test");
        StripeConnectWebhookPort.WebhookEvent result = adapter.parseAndVerify("{}", "invalid_sig");
        assertNull(result);
    }

    @Test
    void parseAndVerifyShouldParseValidPayload() {
        when(stripeProperties.webhookSecret()).thenReturn("whsec_test");

        Event mockEvent = mock(Event.class);
        when(mockEvent.getType()).thenReturn("account.updated");

        Account mockAccount = mock(Account.class);
        when(mockAccount.getId()).thenReturn("acct_123");
        when(mockAccount.getChargesEnabled()).thenReturn(true);
        when(mockAccount.getPayoutsEnabled()).thenReturn(false);

        StripeObject mockHasId = mockAccount;

        com.stripe.model.EventDataObjectDeserializer mockDeserializer = mock(com.stripe.model.EventDataObjectDeserializer.class);
        when(mockDeserializer.getObject()).thenReturn(java.util.Optional.of(mockHasId));
        when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);

        try (MockedStatic<Webhook> mockedWebhook = Mockito.mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(mockEvent);

            StripeConnectWebhookPort.WebhookEvent result = adapter.parseAndVerify("raw_payload", "sig_valid");
            assertNotNull(result);
            assertEquals("account.updated", result.type());
            assertEquals("acct_123", result.stripeAccountId());
            assertTrue(result.chargesEnabled());
            assertFalse(result.payoutsEnabled());
        }
    }

    @Test
    void parseAndVerifyShouldReturnEventWithFalseFlagsWhenAccountIsNull() {
        when(stripeProperties.webhookSecret()).thenReturn("whsec_test");

        Event mockEvent = mock(Event.class);
        when(mockEvent.getType()).thenReturn("account.updated");

        com.stripe.model.EventDataObjectDeserializer mockDeserializer = mock(com.stripe.model.EventDataObjectDeserializer.class);
        when(mockDeserializer.getObject()).thenReturn(java.util.Optional.empty());
        when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);

        try (MockedStatic<Webhook> mockedWebhook = Mockito.mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(mockEvent);

            StripeConnectWebhookPort.WebhookEvent result = adapter.parseAndVerify("raw_payload", "sig_valid");
            assertNotNull(result);
            assertNull(result.stripeAccountId());
            assertFalse(result.chargesEnabled());
            assertFalse(result.payoutsEnabled());
        }
    }
}
