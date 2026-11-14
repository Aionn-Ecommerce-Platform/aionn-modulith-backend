package com.aionn.payment.adapter.rest.controller;

import com.aionn.payment.application.service.StripeConnectWebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StripeConnectWebhookControllerWebTest {

    @Mock
    private StripeConnectWebhookService stripeConnectWebhookService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new StripeConnectWebhookController(stripeConnectWebhookService)).build();
    }

    @Test
    void handleAcceptedWebhookReturnsOk() throws Exception {
        when(stripeConnectWebhookService.handle(any(), any())).thenReturn(true);

        mockMvc.perform(post("/api/v1/payments/webhooks/stripe-connect")
                        .header("Stripe-Signature", "sig")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\": \"account.updated\"}"))
                .andExpect(status().isOk());

        verify(stripeConnectWebhookService).handle(any(), any());
    }

    @Test
    void handleRejectedWebhookReturnsBadRequest() throws Exception {
        when(stripeConnectWebhookService.handle(any(), any())).thenReturn(false);

        mockMvc.perform(post("/api/v1/payments/webhooks/stripe-connect")
                        .header("Stripe-Signature", "sig")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invalid\": true}"))
                .andExpect(status().isBadRequest());
    }
}
