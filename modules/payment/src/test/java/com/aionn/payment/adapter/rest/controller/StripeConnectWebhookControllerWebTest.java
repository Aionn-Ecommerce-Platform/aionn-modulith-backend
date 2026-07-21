package com.aionn.payment.adapter.rest.controller;

import com.aionn.payment.application.port.in.stripeconnect.SyncStripeConnectAccountInputPort;
import com.aionn.payment.application.port.out.stripeconnect.StripeConnectWebhookPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StripeConnectWebhookControllerWebTest {

    @Mock
    private StripeConnectWebhookPort stripeConnectWebhookPort;
    @Mock
    private SyncStripeConnectAccountInputPort syncStripeConnectAccountInputPort;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        StripeConnectWebhookController controller = new StripeConnectWebhookController(
                stripeConnectWebhookPort,
                syncStripeConnectAccountInputPort
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void shouldHandleAccountUpdatedSuccessfully() throws Exception {
        StripeConnectWebhookPort.WebhookEvent event = new StripeConnectWebhookPort.WebhookEvent("account.updated", "acct-1", true, false);
        when(stripeConnectWebhookPort.parseAndVerify(any(), any())).thenReturn(event);

        mockMvc.perform(post("/api/v1/payments/webhooks/stripe-connect")
                        .header("Stripe-Signature", "sig")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\": \"account.updated\"}"))
                .andExpect(status().isOk());

        verify(syncStripeConnectAccountInputPort).execute("acct-1", true, false);
    }

    @Test
    void shouldIgnoreOtherEventTypes() throws Exception {
        StripeConnectWebhookPort.WebhookEvent event = new StripeConnectWebhookPort.WebhookEvent("payment_intent.created", "acct-1", true, true);
        when(stripeConnectWebhookPort.parseAndVerify(any(), any())).thenReturn(event);

        mockMvc.perform(post("/api/v1/payments/webhooks/stripe-connect")
                        .header("Stripe-Signature", "sig")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\": \"payment_intent.created\"}"))
                .andExpect(status().isOk());

        verifyNoInteractions(syncStripeConnectAccountInputPort);
    }

    @Test
    void shouldReturnBadRequestWhenWebhookEventIsNull() throws Exception {
        when(stripeConnectWebhookPort.parseAndVerify(any(), any())).thenReturn(null);

        mockMvc.perform(post("/api/v1/payments/webhooks/stripe-connect")
                        .header("Stripe-Signature", "sig")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invalid\": true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenAccountUpdatedButStripeAccountIdIsNull() throws Exception {
        StripeConnectWebhookPort.WebhookEvent event = new StripeConnectWebhookPort.WebhookEvent("account.updated", null, false, false);
        when(stripeConnectWebhookPort.parseAndVerify(any(), any())).thenReturn(event);

        mockMvc.perform(post("/api/v1/payments/webhooks/stripe-connect")
                        .header("Stripe-Signature", "sig")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\": \"account.updated\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(syncStripeConnectAccountInputPort);
    }
}
